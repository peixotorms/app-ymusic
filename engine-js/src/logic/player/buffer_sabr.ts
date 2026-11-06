import { playbackBuffer } from "./buffering";
import { videoIdForSabrUrl } from "./player_res";
import { isAudioOnly } from "./audio_only";
import { isYmLive } from "./live";
import { readFields, writeFields, parseBufferedRange, umpPart } from "../yt/sabr_abr";
import type { Field } from "../yt/sabr_abr";
import { buildHeaders } from "../../protocol/http/headers";
import { BufferedResponse } from "../../protocol/http/intercept";

function bytes(fields: Field[], number: number): Uint8Array | null {
  const field = fields.find((f) => f.field === number && f.wire === 2);
  return field && field.value instanceof Uint8Array ? field.value : null;
}

function number(fields: Field[], key: number): number {
  const field = fields.find((f) => f.field === key && f.wire === 0);
  return field && typeof field.value === "number" ? field.value : 0;
}

function string(value: string): Uint8Array {
  return new TextEncoder().encode(value);
}

function contiguousEnd(
  ranges: { startMs: number; durationMs: number }[],
  position: number,
): number {
  let end = position;
  for (const range of ranges.sort((a, b) => a.startMs - b.startMs)) {
    if (range.startMs > end + 100) break;
    if (range.startMs + range.durationMs > end) end = range.startMs + range.durationMs;
  }
  return end;
}

export function bufferBackoff(url: string, body: unknown): HttpResponse | null {
  if (!playbackBuffer.enabled || isYmLive() || !(body instanceof Uint8Array)) return null;
  const videoId = videoIdForSabrUrl(url);
  if (!videoId) return null;
  const fields = readFields(body);
  if (!fields) return null;
  const cas = bytes(fields, 1);
  if (!cas) return null;
  const position = number(readFields(cas) || [], 28);
  const audio = new Set<number>();
  const video = new Set<number>();
  for (const field of fields) {
    if ((field.field === 16 || field.field === 17) && field.value instanceof Uint8Array) {
      const itag = number(readFields(field.value) || [], 1);
      (field.field === 16 ? audio : video).add(itag);
    }
  }
  if (!audio.size) return null;
  const audioRanges: { startMs: number; durationMs: number }[] = [];
  const videoRanges: { startMs: number; durationMs: number }[] = [];
  for (const field of fields) {
    if (field.field !== 3 || !(field.value instanceof Uint8Array)) continue;
    const range = parseBufferedRange(field.value);
    if (!range) continue;
    if (audio.has(range.itag)) audioRanges.push(range);
    if (video.has(range.itag)) videoRanges.push(range);
  }
  let end = contiguousEnd(audioRanges, position);
  if (!isAudioOnly() && video.size) end = Math.min(end, contiguousEnd(videoRanges, position));
  const budget = playbackBuffer.budget(videoId, position, end);
  if (!budget || budget.backoffMs <= 0) return null;
  const context = bytes(fields, 19);
  const cookie = context ? bytes(readFields(context) || [], 3) : null;
  const policy: Field[] = [
    { field: 1, wire: 0, value: budget.targetMs },
    { field: 2, wire: 0, value: budget.targetMs },
    { field: 4, wire: 0, value: budget.backoffMs },
    { field: 8, wire: 2, value: string(videoId) },
  ];
  if (cookie) policy.push({ field: 7, wire: 2, value: cookie });
  const data = umpPart(35, writeFields(policy));
  const empty: HttpResponse = {
    status: 200,
    statusText: "OK",
    ok: true,
    url: url,
    headers: buildHeaders([["Content-Type", "application/vnd.yt-ump"]]),
    text: async () => "",
    bytes: async () => new Uint8Array(0),
    jsonObject: async () => ({}),
    jsonArray: async () => [],
    stream: () => ({ read: async () => -1 }),
    cancel: () => {},
    close: () => {},
  };
  return new BufferedResponse(empty, data);
}

export function bufferPolicyPart(url: string, original: Uint8Array | null): Uint8Array[] {
  if (!original || isYmLive()) return [];
  const target = playbackBuffer.target(videoIdForSabrUrl(url));
  if (target === null) return [];
  const fields = readFields(original);
  if (!fields) return [];
  const next = fields.filter((f) => f.field !== 1 && f.field !== 2);
  next.push({ field: 1, wire: 0, value: target }, { field: 2, wire: 0, value: target });
  return [umpPart(35, writeFields(next))];
}
