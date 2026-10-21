import { createHash } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';

const BASE = 'https://trainschedule.lk';
const USER_AGENT = 'TrainLankaCrawler/1.0 (+https://github.com/MithunWijayasiri/TrainLanka)';
const DELAY_MS = 1000;
const ATTEMPTS = 3;
const TIMEOUT_MS = 30_000;
// Dev only: set CACHE_DIR to reuse responses across runs.
const CACHE_DIR = process.env.CACHE_DIR;

let lastRequestAt = 0;
let requestCount = 0;

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

export const requestsMade = () => requestCount;

export async function get(path: string): Promise<string> {
  const cacheFile = CACHE_DIR && `${CACHE_DIR}/${createHash('sha1').update(path).digest('hex')}.html`;
  if (cacheFile) {
    const cached = await readFile(cacheFile, 'utf8').catch(() => undefined);
    if (cached !== undefined) return cached;
  }

  for (let attempt = 1; ; attempt++) {
    await sleep(lastRequestAt + DELAY_MS - Date.now());
    lastRequestAt = Date.now();
    requestCount++;

    let retryReason: string;
    try {
      const res = await fetch(BASE + path, {
        headers: { 'User-Agent': USER_AGENT },
        signal: AbortSignal.timeout(TIMEOUT_MS),
      });
      if (res.ok) {
        const body = await res.text();
        if (cacheFile) {
          await mkdir(CACHE_DIR!, { recursive: true });
          await writeFile(cacheFile, body);
        }
        return body;
      }
      if (res.status < 500) throw new Error(`HTTP ${res.status} for ${path}`);
      retryReason = `HTTP ${res.status}`;
    } catch (e) {
      if (e instanceof Error && e.message.startsWith('HTTP ')) throw e;
      retryReason = String(e);
    }
    if (attempt === ATTEMPTS) throw new Error(`${retryReason} for ${path} after ${ATTEMPTS} attempts`);
    await sleep(5000 * attempt);
  }
}
