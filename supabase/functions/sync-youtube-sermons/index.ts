// Supabase Edge Function: YouTube RSS 피드에서 설교 영상 자동 동기화
//
// 배포: supabase functions deploy sync-youtube-sermons
// 테스트: curl -X POST https://YOUR_PROJECT.supabase.co/functions/v1/sync-youtube-sermons

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

// YouTube RSS 피드 파싱
async function fetchYouTubeRSS(channelId: string) {
  const rssUrl = `https://www.youtube.com/feeds/videos.xml?channel_id=${channelId}`;
  const response = await fetch(rssUrl);

  if (!response.ok) {
    throw new Error(`RSS fetch failed: ${response.status}`);
  }

  const xmlText = await response.text();
  const entries: any[] = [];

  // XML 파싱 (정규식)
  const entryRegex = /<entry>([\s\S]*?)<\/entry>/g;
  let match;

  while ((match = entryRegex.exec(xmlText)) !== null) {
    const entryXml = match[1];

    const videoId = entryXml.match(/<yt:videoId>(.*?)<\/yt:videoId>/)?.[1];
    const title = entryXml.match(/<title>(.*?)<\/title>/)?.[1];
    const published = entryXml.match(/<published>(.*?)<\/published>/)?.[1];
    const description = entryXml.match(/<media:description>([\s\S]*?)<\/media:description>/)?.[1]?.trim() || "";

    if (videoId && title && published) {
      entries.push({
        videoId,
        title: decodeHtml(title),
        description: decodeHtml(description),
        publishedAt: published,
        thumbnailUrl: `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
      });
    }
  }

  return entries;
}

function decodeHtml(text: string): string {
  return text
    .replace(/&amp;/g, "&")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'");
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    // 채널 설정 가져오기
    const { data: channels, error: channelError } = await supabase
      .from("youtube_channel_config")
      .select("*")
      .eq("is_active", true);

    if (channelError) throw channelError;
    if (!channels?.length) {
      return new Response(
        JSON.stringify({ message: "No channels configured" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    let totalSynced = 0;

    for (const channel of channels) {
      const entries = await fetchYouTubeRSS(channel.channel_id);

      for (const entry of entries) {
        const { error } = await supabase
          .from("sermons")
          .upsert({
            youtube_video_id: entry.videoId,
            title: entry.title,
            preacher: channel.default_preacher,
            description: entry.description,
            thumbnail_url: entry.thumbnailUrl,
            published_at: entry.publishedAt,
          }, { onConflict: "youtube_video_id" });

        if (!error) totalSynced++;
      }

      // 동기화 시간 업데이트
      await supabase
        .from("youtube_channel_config")
        .update({ last_sync_at: new Date().toISOString() })
        .eq("id", channel.id);
    }

    return new Response(
      JSON.stringify({ success: true, synced: totalSynced }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );

  } catch (error) {
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
