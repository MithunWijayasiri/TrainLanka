import * as cheerio from 'cheerio';

export const normalizeName = (s: string) => s.trim().replace(/\s+/g, ' ');

export type StationListItem = { id: number; name: string };

export function parseStationList(html: string): StationListItem[] {
  const $ = cheerio.load(html);
  const options = $('select[name="ScheduleModel.TrainModel.SelectedStartStaionID"] option[value!=""]');
  return options.toArray().map((o) => ({ id: Number($(o).attr('value')), name: normalizeName($(o).text()) }));
}

export type StationPageTrain = { linkCode: string; no: string; from: string; to: string; days: string[] };

export function parseStationPage(html: string): { phone?: string; trains: StationPageTrain[] } {
  const $ = cheerio.load(html);
  if (!$('.train-schedules').length) throw new Error('Station page has no .train-schedules section');

  const trains: StationPageTrain[] = [];
  for (const tr of $('.train-schedules tbody tr').toArray()) {
    const tds = $(tr).find('td');
    if (tds.length === 0) continue;
    if (tds.length !== 5) throw new Error(`Station row has ${tds.length} cells: ${$(tr).text()}`);

    const route = tds.eq(0).text().split('→').map(normalizeName);
    const link = tds.eq(1).find('a');
    const href = link.attr('href') ?? '';
    const linkCode = href.split('/')[2];
    if (route.length !== 2 || !href.startsWith('/stops/') || !linkCode) {
      throw new Error(`Unparseable station row: ${$(tr).text()}`);
    }
    trains.push({
      linkCode,
      no: link.text().trim(),
      from: route[0],
      to: route[1],
      days: tds.eq(2).text().split(',').map((d) => d.trim()).filter(Boolean),
    });
  }

  const phone = $('body').text().match(/Contact:\s*(\d{9,10})/)?.[1];
  return { phone, trains };
}

export type Stop = { stationId: number; name: string; arr?: number; dep?: number; lat?: number; lng?: number };

const toMinutes = (t: string | null) => {
  if (t === null) return undefined;
  const m = t.match(/^(\d{2}):(\d{2}):\d{2}$/);
  if (!m) throw new Error(`Unparseable time "${t}"`);
  return Number(m[1]) * 60 + Number(m[2]);
};

const toCoord = (s: string | null) => {
  const n = s === null ? NaN : Number(s);
  return Number.isFinite(n) && n !== 0 ? n : undefined;
};

export function parseStops(html: string): Stop[] {
  const json = html.match(/var stationsData = (\[.*?\]);/s)?.[1];
  if (!json) throw new Error('Stops page has no stationsData');
  const raw = JSON.parse(json) as {
    endStationID: number;
    endStation: string;
    fromArrivalTime: string | null;
    fromDepartureTime: string | null;
    latitude: string | null;
    longitude: string | null;
  }[];
  return raw.map((s) => ({
    stationId: s.endStationID,
    name: normalizeName(s.endStation),
    arr: toMinutes(s.fromArrivalTime),
    dep: toMinutes(s.fromDepartureTime),
    lat: toCoord(s.latitude),
    lng: toCoord(s.longitude),
  }));
}

/** Schedule page rows: stops link code → train type. */
export function parseScheduleTypes(html: string): Map<string, string> {
  const $ = cheerio.load(html);
  const types = new Map<string, string>();
  for (const td of $('td.train-type-column').toArray()) {
    const href = $(td).parent().find('a[href^="/stops/"]').attr('href');
    const code = href?.split('/')[2];
    if (!code) throw new Error(`Schedule row without stops link: ${$(td).parent().text()}`);
    types.set(code, normalizeName($(td).text()));
  }
  return types;
}
