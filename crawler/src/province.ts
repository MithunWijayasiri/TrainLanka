// geoBoundaries gbOpen LKA ADM1 (OSM-derived, ODbL 1.0), pinned commit.
const URL =
  'https://github.com/wmgeolab/geoBoundaries/raw/9469f09/releaseData/gbOpen/LKA/ADM1/geoBoundaries-LKA-ADM1.geojson';
// Coastal stations can sit just outside the simplified coastline.
const MAX_OUTSIDE_METERS = 2000;

type Ring = [number, number][];
type Province = { name: string; polygons: Ring[][] };

export async function loadProvinces(): Promise<Province[]> {
  const res = await fetch(URL, { signal: AbortSignal.timeout(30_000) });
  if (!res.ok) throw new Error(`HTTP ${res.status} for province boundaries`);
  const geo = (await res.json()) as {
    features: { properties: { shapeName: string }; geometry: { type: string; coordinates: unknown } }[];
  };
  const provinces = geo.features.map((f) => ({
    name: f.properties.shapeName.replace(/ Province$/, ''),
    polygons:
      f.geometry.type === 'Polygon' ? [f.geometry.coordinates as Ring[]] : (f.geometry.coordinates as Ring[][]),
  }));
  if (provinces.length !== 9) throw new Error(`Expected 9 provinces, got ${provinces.length}`);
  return provinces;
}

function insideRing([x, y]: [number, number], ring: Ring): boolean {
  let inside = false;
  for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
    const [xi, yi] = ring[i];
    const [xj, yj] = ring[j];
    if (yi > y !== yj > y && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi) inside = !inside;
  }
  return inside;
}

function distanceToRingMeters([lng, lat]: [number, number], ring: Ring): number {
  // Equirectangular projection; accurate enough at a few km.
  const kx = 111320 * Math.cos((lat * Math.PI) / 180);
  const ky = 110540;
  let min = Infinity;
  for (let i = 1; i < ring.length; i++) {
    const ax = (ring[i - 1][0] - lng) * kx, ay = (ring[i - 1][1] - lat) * ky;
    const bx = (ring[i][0] - lng) * kx, by = (ring[i][1] - lat) * ky;
    const dx = bx - ax, dy = by - ay;
    const t = Math.max(0, Math.min(1, -(ax * dx + ay * dy) / (dx * dx + dy * dy || 1)));
    min = Math.min(min, Math.hypot(ax + t * dx, ay + t * dy));
  }
  return min;
}

export function provinceOf(provinces: Province[], lat: number, lng: number): string | undefined {
  const p: [number, number] = [lng, lat];
  const inside = provinces.find((pr) =>
    pr.polygons.some(([outer, ...holes]) => insideRing(p, outer) && !holes.some((h) => insideRing(p, h))),
  );
  if (inside) return inside.name;

  let best: { name: string; d: number } | undefined;
  for (const pr of provinces) {
    for (const [outer] of pr.polygons) {
      const d = distanceToRingMeters(p, outer);
      if (!best || d < best.d) best = { name: pr.name, d };
    }
  }
  return best && best.d <= MAX_OUTSIDE_METERS ? best.name : undefined;
}
