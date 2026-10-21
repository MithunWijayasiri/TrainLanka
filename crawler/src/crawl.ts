import { readFile, writeFile } from 'node:fs/promises';
import { routeCode, stationCode, stopsCode, trainIdFromStationLink, trainIdFromStopsCode } from './codes.ts';
import { get, requestsMade } from './http.ts';
import { parseScheduleTypes, parseStationList, parseStationPage, parseStops } from './parse.ts';
import { loadProvinces, provinceOf } from './province.ts';

const DATA_DIR = new URL('../../data/', import.meta.url);
const OUTPUT = new URL('timetable.json', DATA_DIR);
const WEEKDAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
const KNOWN_DAYS = [...WEEKDAYS, 'Poya', 'Holi'];
const MAX_GAP_MINUTES = 6 * 60;
const MAX_CLAMP_MINUTES = 15;
const MIDNIGHT_WRAP_MINUTES = 12 * 60;
// Sanity floor: a site change that hides most trains must fail, not publish a near-empty timetable.
const MIN_TRAINS = 200;
const MAX_UNLOCATED = 20;

type Extra = { aliases?: string[]; province?: string; lat?: number; lng?: number };
type Train = { id: number; no: string; from: number; to: number; days: string[] };
/** [stationId, arrival, departure]; minutes since midnight of the train's start day, can exceed 1440. */
type StopRow = [number, number, number];

const errors: string[] = [];
const extra = JSON.parse(await readFile(new URL('stations-extra.json', DATA_DIR), 'utf8')) as Record<string, Extra>;
const provinces = await loadProvinces();

// Stations
const stationList = parseStationList(await get('/'));
if (stationList.length < 300) throw new Error(`Only ${stationList.length} stations on home page`);
const nameById = new Map(stationList.map((s) => [s.id, s.name]));
const idByName = new Map<string, number>();
for (const s of stationList) {
  const key = s.name.toLowerCase();
  if (idByName.has(key)) throw new Error(`Duplicate station name "${s.name}"`);
  idByName.set(key, s.id);
}
const stationId = (name: string) => {
  const id = idByName.get(name.toLowerCase());
  if (id === undefined) throw new Error(`Unknown station "${name}"`);
  return id;
};
Object.keys(extra).forEach(stationId);
console.log(`${stationList.length} stations`);

// Station pages → trains
const trains = new Map<number, Train>();
const phones = new Map<number, string>();
for (const [i, s] of stationList.entries()) {
  if (i % 50 === 0) console.log(`Station pages ${i}/${stationList.length}`);
  const page = parseStationPage(await get(`/station/${stationCode(s.id)}/x`));
  if (page.phone) phones.set(s.id, page.phone);
  for (const t of page.trains) {
    const unknownDay = t.days.find((d) => !KNOWN_DAYS.includes(d));
    if (unknownDay) throw new Error(`Unknown running day "${unknownDay}" for train ${t.no}`);
    const train = { id: trainIdFromStationLink(t.linkCode), no: t.no, from: stationId(t.from), to: stationId(t.to), days: t.days };
    const seen = trains.get(train.id);
    if (seen && JSON.stringify(seen) !== JSON.stringify(train)) {
      throw new Error(`Train ${train.no} differs between station pages: ${JSON.stringify(seen)} vs ${JSON.stringify(train)}`);
    }
    trains.set(train.id, train);
  }
}
console.log(`${trains.size} trains`);

const dropped: string[] = [];
const routes = new Map<number, StopRow[]>();
const drop = (t: Train, reason: string) => {
  trains.delete(t.id);
  routes.delete(t.id);
  dropped.push(`Train ${t.no} (#${t.id}) ${nameById.get(t.from)} → ${nameById.get(t.to)}: ${reason}`);
};

// The site's search never shows trains that don't run on any weekday, so neither do we.
for (const t of [...trains.values()]) {
  if (!t.days.some((d) => WEEKDAYS.includes(d))) drop(t, `no weekday running days (${t.days.join(',') || 'none'})`);
}

// Stops pages → full routes
class DropTrain extends Error {}
let clampedTimes = 0;
const coords = new Map<number, { lat: number; lng: number }>();
for (const [i, t] of [...trains.values()].entries()) {
  if (i % 50 === 0) console.log(`Stops pages ${i}/${trains.size}`);
  const stops = parseStops(await get(`/stops/${stopsCode(t.id, t.from, t.to)}/x`));
  if (stops.length < 2 || stops[0].stationId !== t.from || stops.at(-1)!.stationId !== t.to) {
    errors.push(`Train ${t.no}: stops page does not run ${nameById.get(t.from)} → ${nameById.get(t.to)}`);
    continue;
  }

  let prev: number | undefined;
  let dayOffset = 0;
  const unwrap = (m: number, at: string) => {
    let v = m + dayOffset;
    if (prev !== undefined && v < prev) {
      if (prev - v >= MIDNIGHT_WRAP_MINUTES) {
        dayOffset += 1440;
        v += 1440;
      } else if (prev - v <= MAX_CLAMP_MINUTES) {
        clampedTimes++;
        v = prev;
      } else {
        throw new DropTrain(`time goes back ${prev - v} min at ${at}`);
      }
    }
    if (prev !== undefined && v - prev > MAX_GAP_MINUTES) throw new DropTrain(`${v - prev} min gap at ${at}`);
    prev = v;
    return v;
  };
  try {
    routes.set(
      t.id,
      stops.map((st, i): StopRow => {
        if (!nameById.has(st.stationId)) throw new Error(`Train ${t.no}: unknown station ID ${st.stationId}`);
        // Source fills origin arrival and terminus departure from the train's previous/next service.
        const arr = i === 0 ? st.dep : (st.arr ?? st.dep);
        const dep = i === stops.length - 1 ? st.arr : (st.dep ?? st.arr);
        if (arr === undefined || dep === undefined) throw new DropTrain(`no time at ${st.name}`);
        if (st.lat !== undefined && st.lng !== undefined && !coords.has(st.stationId)) {
          coords.set(st.stationId, { lat: st.lat, lng: st.lng });
        }
        return [st.stationId, unwrap(arr, st.name), unwrap(dep, st.name)];
      }),
    );
  } catch (e) {
    if (!(e instanceof DropTrain)) throw e;
    drop(t, e.message);
  }
}

// Schedule pages → train types. Pages are per date, so query dates the trains run.
const nextDates = Array.from({ length: 14 }, (_, i) => {
  const d = new Date(Date.now() + i * 86_400_000);
  return {
    iso: new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Colombo' }).format(d),
    weekday: new Intl.DateTimeFormat('en-US', { timeZone: 'Asia/Colombo', weekday: 'short' }).format(d),
  };
});
const types = new Map<number, string>();
const byRoute = Map.groupBy(trains.values(), (t) => `${t.from}-${t.to}`);
let routeIndex = 0;
for (const group of byRoute.values()) {
  if (routeIndex++ % 50 === 0) console.log(`Schedule routes ${routeIndex - 1}/${byRoute.size}`);
  const { from, to } = group[0];
  const pending = new Set(group.map((t) => t.id));
  for (const date of nextDates) {
    if (!group.some((t) => pending.has(t.id) && t.days.includes(date.weekday))) continue;
    const found = parseScheduleTypes(await get(`/schedule/${routeCode(from, to)}/${date.iso}/x`));
    for (const [code, type] of found) {
      const id = trainIdFromStopsCode(code);
      if (pending.delete(id)) types.set(id, type);
    }
    if (!pending.size) break;
  }
  for (const id of pending) drop(trains.get(id)!, 'not in site search for next 14 days');
}

console.log(`Dropped ${dropped.length} trains:\n${dropped.join('\n')}`);
console.log(`Clamped ${clampedTimes} backward times`);
if (trains.size < MIN_TRAINS) errors.push(`Only ${trains.size} trains left (minimum ${MIN_TRAINS})`);

// Output
const servedIds = new Set([...routes.values()].flatMap((r) => r.map(([s]) => s)));
const provinceNames = new Set(provinces.map((p) => p.name));
const unlocated: string[] = [];
const stationsOut = [...servedIds]
  .map((id) => {
    const name = nameById.get(id)!;
    const x = extra[name] ?? {};
    const lat = x.lat ?? coords.get(id)?.lat ?? null;
    const lng = x.lng ?? coords.get(id)?.lng ?? null;
    if (x.province && !provinceNames.has(x.province)) errors.push(`${name}: unknown province override "${x.province}"`);
    const province = x.province ?? (lat !== null && lng !== null ? provinceOf(provinces, lat, lng) : undefined) ?? null;
    if (lat === null || province === null) unlocated.push(`${name}: ${lat === null ? 'no coordinates' : 'outside all provinces'}`);
    return { id, name, province, lat, lng, aliases: x.aliases ?? [], phone: phones.get(id) ?? null };
  })
  .sort((a, b) => a.name.localeCompare(b.name));
console.log(`${unlocated.length} stations without coordinates or province:\n${unlocated.join('\n')}`);
if (unlocated.length > MAX_UNLOCATED) errors.push(`${unlocated.length} stations unlocated (maximum ${MAX_UNLOCATED})`);

const trainsOut = [...trains.values()]
  .filter((t) => routes.has(t.id) && types.has(t.id))
  .map((t) => ({ id: t.id, no: t.no, type: types.get(t.id)!, days: t.days, stops: routes.get(t.id)! }))
  .sort((a, b) => a.stops[0][2] - b.stops[0][2] || a.no.localeCompare(b.no));

if (errors.length) {
  console.error(`${errors.length} errors:\n${errors.join('\n')}`);
  process.exit(1);
}

const content = { stations: stationsOut, trains: trainsOut };
const previous = await readFile(OUTPUT, 'utf8').then(JSON.parse, () => undefined);
if (previous && JSON.stringify({ stations: previous.stations, trains: previous.trains }) === JSON.stringify(content)) {
  console.log(`No changes (${requestsMade()} requests)`);
} else {
  const version = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Colombo' }).format(new Date());
  const lines = (items: unknown[]) => items.map((i) => '  ' + JSON.stringify(i)).join(',\n');
  await writeFile(
    OUTPUT,
    `{\n"version": "${version}",\n"stations": [\n${lines(stationsOut)}\n],\n"trains": [\n${lines(trainsOut)}\n]\n}\n`,
  );
  console.log(`Wrote ${stationsOut.length} stations, ${trainsOut.length} trains (${requestsMade()} requests)`);
}
