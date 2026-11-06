export interface BufferPlayback {
  videoId: string;
  positionMs: number;
  durationMs: number;
  bufferedMs: number;
  playing: boolean;
  live: boolean;
}

export interface BufferBudget {
  targetMs: number;
  backoffMs: number;
}

interface ActiveTransfer {
  videoId: string;
  cancel: () => void;
}

export class PlaybackBufferPolicy {
  enabled = false;
  private current = "";
  private next = "";
  private audioOnly = true;
  private audioTargetMs = 120000;
  private playedMs = 0;
  private previousAt = 0;
  private previousPlaying = false;
  private previousPosition = 0;
  private durationMs = 0;
  private live = false;
  private filling = true;
  private nextFilling = true;
  private retired = new Set<string>();
  private transfers = new Set<ActiveTransfer>();

  constructor(private readonly clock: () => number = Date.now) {}

  load(videoId: string, audioOnly: boolean, audioSeconds: unknown): void {
    this.enabled = typeof audioSeconds === "number" && Number.isFinite(audioSeconds);
    this.configure(audioOnly, audioSeconds);
    if (this.current && this.current !== videoId) this.retire(this.current);
    if (this.next && this.next !== videoId) this.retire(this.next);
    this.current = videoId;
    this.next = "";
    this.retired.delete(videoId);
    this.playedMs = 0;
    this.previousAt = this.clock();
    this.previousPlaying = false;
    this.previousPosition = 0;
    this.durationMs = 0;
    this.live = false;
    this.filling = true;
    this.nextFilling = true;
  }

  configure(audioOnly: boolean, audioSeconds: unknown): void {
    this.audioOnly = audioOnly;
    if (typeof audioSeconds === "number" && Number.isFinite(audioSeconds)) {
      this.audioTargetMs = Math.max(30, Math.min(120, audioSeconds)) * 1000;
    }
    this.filling = true;
  }

  prepare(videoId: string | null): void {
    if (this.next && this.next !== videoId && this.next !== this.current) this.retire(this.next);
    this.next = videoId || "";
    if (videoId) this.retired.delete(videoId);
    this.nextFilling = true;
  }

  canPrepare(videoId: string): boolean {
    return !this.enabled || (this.next === videoId && this.playedMs >= 5000 && !this.live);
  }

  isNext(videoId: string): boolean {
    return this.next === videoId;
  }

  metadata(videoId: string): void {
    this.retired.delete(videoId);
  }

  stop(): void {
    if (this.current) this.retire(this.current);
    if (this.next) this.retire(this.next);
    this.current = this.next = "";
    this.previousPlaying = false;
  }

  seek(): void {
    this.previousPlaying = false;
    this.previousAt = this.clock();
    this.filling = true;
  }

  observe(playback: BufferPlayback): void {
    if (!this.enabled || playback.videoId !== this.current) return;
    const at = this.clock();
    const elapsed = Math.max(0, at - this.previousAt);
    const moved = playback.positionMs - this.previousPosition;
    if (this.previousPlaying && playback.playing && moved > 0 && moved <= elapsed * 4 + 500) {
      this.playedMs += elapsed;
    }
    this.previousAt = at;
    this.previousPosition = playback.positionMs;
    this.previousPlaying = playback.playing;
    this.durationMs = playback.durationMs;
    this.live = playback.live;
  }

  target(videoId: string): number | null {
    if (!this.enabled || this.live) return null;
    if (videoId === this.next) return 15000;
    if (videoId !== this.current) return null;
    if (!this.audioOnly) return 30000;
    return this.playedMs < 5000 ? 15000 : this.audioTargetMs;
  }

  budget(videoId: string, positionMs: number, bufferedEndMs: number): BufferBudget | null {
    if (!this.enabled) return null;
    if (this.retired.has(videoId)) return { targetMs: 15000, backoffMs: 2000 };
    const desired = this.target(videoId);
    if (desired === null) return null;
    const isCurrent = videoId === this.current;
    const remaining =
      isCurrent && this.durationMs > 0 ? Math.max(0, this.durationMs - positionMs) : Infinity;
    const high = Math.min(desired, remaining);
    const ahead = Math.max(0, bufferedEndMs - positionMs);
    const low = Math.max(0, high - Math.min(15000, high / 2));
    let filling = isCurrent ? this.filling : this.nextFilling;
    if (ahead >= high - 100 && high > 0) filling = false;
    else if (ahead <= low) filling = true;
    if (isCurrent) this.filling = filling;
    else this.nextFilling = filling;
    const grace = isCurrent && this.audioOnly && this.playedMs < 5000;
    return {
      targetMs: desired,
      backoffMs:
        filling && high > 0
          ? 0
          : Math.max(250, Math.min(isCurrent && !grace ? 15000 : 1000, ahead - low)),
    };
  }

  track(videoId: string, cancel: (() => void) | undefined): () => void {
    if (!this.enabled || !cancel || (videoId !== this.current && videoId !== this.next))
      return () => {};
    const transfer = { videoId: videoId, cancel: cancel };
    this.transfers.add(transfer);
    return () => {
      this.transfers.delete(transfer);
    };
  }

  private retire(videoId: string): void {
    this.retired.add(videoId);
    if (this.retired.size > 32) this.retired.delete(this.retired.values().next().value!);
    for (const transfer of this.transfers) {
      if (transfer.videoId === videoId) {
        this.transfers.delete(transfer);
        transfer.cancel();
      }
    }
  }
}

export const playbackBuffer = new PlaybackBufferPolicy();

export function configurePlayerBuffer(rec: Record<string, unknown>): void {
  if (!playbackBuffer.enabled) return;
  const details = rec["videoDetails"] as Record<string, unknown> | undefined;
  if (
    details?.["isLive"] === true ||
    playbackBuffer.target(String(details?.["videoId"] || "")) === null
  )
    return;
  const pc = (rec["playerConfig"] || {}) as Record<string, unknown>;
  const common = (pc["mediaCommonConfig"] || {}) as Record<string, unknown>;
  const dynamic = (common["dynamicReadaheadConfig"] || {}) as Record<string, unknown>;
  dynamic["minReadAheadMediaTimeMs"] = 120000;
  dynamic["maxReadAheadMediaTimeMs"] = 120000;
  dynamic["readAheadGrowthRateMs"] = 120000;
  common["dynamicReadaheadConfig"] = dynamic;
  pc["mediaCommonConfig"] = common;
  rec["playerConfig"] = pc;
}
