// trainschedule.lk URL codes, reverse-engineered: base-36 IDs over this alphabet, most significant first.
const ALPHA = 'abcdefghijklmnopqrstuvwxyz0123456789';

export function enc3(n: number): string {
  if (!Number.isInteger(n) || n < 0 || n >= 36 ** 3) throw new Error(`Cannot encode ${n}`);
  return ALPHA[Math.floor(n / 1296)] + ALPHA[Math.floor(n / 36) % 36] + ALPHA[n % 36];
}

export function dec3(s: string): number {
  if (!/^[a-z0-9]{3}$/.test(s)) throw new Error(`Cannot decode "${s}"`);
  return [...s].reduce((n, c) => n * 36 + ALPHA.indexOf(c), 0);
}

const check = (n: number) => ALPHA[(n % 10) * 2];
const reverse = (s: string) => [...s].reverse().join('');

/** `/schedule/{code}`: trains from → to. */
export const routeCode = (from: number, to: number) => enc3(from) + check(to) + reverse(enc3(to));

/** `/stops/{code}`: one train's stops between from and to, inclusive. */
export const stopsCode = (train: number, from: number, to: number) => enc3(train) + enc3(from) + enc3(to);

/** `/station/{code}`: trains passing a station. */
export function stationCode(id: number): string {
  const v = id + 15;
  return ALPHA[v % 36] + ALPHA[Math.floor(v / 36) + 3] + 'a' + check(id);
}

/** Internal train ID from a station page's `/stops/{code}/...-for-{trainNo}` link. */
export function trainIdFromStationLink(code: string): number {
  if (code.length !== 7) throw new Error(`Unexpected train link code "${code}"`);
  const id = dec3(reverse(code.slice(4)));
  if (code[3] !== check(id)) throw new Error(`Check char mismatch in train link code "${code}"`);
  return id;
}

/** Internal train ID from a schedule page's stops link code. */
export const trainIdFromStopsCode = (code: string) => dec3(code.slice(0, 3));
