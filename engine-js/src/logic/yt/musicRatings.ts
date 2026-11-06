import { onAccept } from "../../transport";
import { commandHandler, JsEngineErrorImpl } from "../../protocol/command";
import { hostLogger } from "../../protocol/logger";
import { isSignedIn } from "./login";
import { asString, get, isObj, walk } from "./scan";
import { musicJson } from "./ytmusic";
import type { WireMusicRating } from "./wire";

const ENDPOINTS: Record<string, string> = {
  "LIKE": "like/like",
  "DISLIKE": "like/dislike",
  "INDIFFERENT": "like/removelike",
};

function required(args: Record<string, unknown>, key: string): string {
  const value = asString(args[key]);
  if (!value) throw JsEngineErrorImpl.parse("ytmusic rating needs " + key);
  return value;
}

export function musicRatingOf(root: Record<string, unknown>, videoId: string): string | undefined {
  let status: string | undefined;
  walk(root, (key, value) => {
    if (!isObj(value)) return;
    if (key === "likeButtonRenderer" && get(value, "target", "videoId") === videoId) {
      const rating = asString(value["likeStatus"]);
      if (rating !== undefined && ENDPOINTS[rating] !== undefined) status = rating;
      return true;
    }
    if (key !== "playlistPanelVideoRenderer" || value["videoId"] !== videoId) return;
    walk(get(value, "menu"), (menuKey, item) => {
      if (menuKey !== "menuToggleServiceItemRenderer") return;
      const next = get(item, "defaultServiceEndpoint", "likeEndpoint");
      if (get(next, "target", "videoId") !== videoId) return;
      const target = asString(get(next, "status"));
      if (status === undefined && target === "INDIFFERENT") status = "LIKE";
      else if (status === undefined && target === "LIKE") status = "INDIFFERENT";
      return true;
    });
  });
  return status;
}

onAccept(
  "ytmusic/video-rating",
  commandHandler(async (src, out) => {
    const args = await src.readJsonObject();
    const videoId = required(args, "videoId");
    let rating: WireMusicRating = { "likeStatus": "INDIFFERENT", "canRate": false };
    if (await isSignedIn()) {
      const root = await musicJson(
        "next",
        { "videoId": videoId },
        hostLogger(args, "ytmusic/video-rating"),
      );
      const status = musicRatingOf(root, videoId);
      if (status === undefined)
        throw new JsEngineErrorImpl("YouTube Music omitted the requested song's rating", "E20165");
      rating = { "likeStatus": status, "canRate": true };
    }
    const pending = out.writeObjectAsJson(rating);
    if (pending) await pending;
  }),
);

onAccept(
  "ytmusic/video/rate",
  commandHandler(async (src, out) => {
    const args = await src.readJsonObject();
    const videoId = required(args, "videoId");
    const endpoint = ENDPOINTS[required(args, "status")];
    if (endpoint === undefined) throw JsEngineErrorImpl.parse("invalid YouTube Music rating");
    if (!(await isSignedIn()))
      throw new JsEngineErrorImpl("YouTube Music rating requires sign-in", "E20166");
    const root = await musicJson(
      endpoint,
      { "target": { "videoId": videoId } },
      hostLogger(args, "ytmusic/video/rate"),
    );
    if (
      root["error"] !== undefined ||
      get(root, "responseContext", "mainAppWebResponseContext", "loggedOut") === true
    ) {
      throw new JsEngineErrorImpl("YouTube Music rejected the rating", "E20167");
    }
    const pending = out.writeObjectAsJson({});
    if (pending) await pending;
  }),
);
