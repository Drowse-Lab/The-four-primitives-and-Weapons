// scripts/tweet.js
// pushのたびにX(Twitter)へコミット情報を投稿するスクリプト
// .github/workflows/tweet-on-push.yml から呼び出される

const { TwitterApi } = require("twitter-api-v2");

const client = new TwitterApi({
  appKey: process.env.TWITTER_API_KEY,
  appSecret: process.env.TWITTER_API_SECRET,
  accessToken: process.env.TWITTER_ACCESS_TOKEN,
  accessSecret: process.env.TWITTER_ACCESS_SECRET,
});

// コミットメッセージが長すぎる場合は切り詰める
// X は URL を 23 文字換算するので本文は ~230 文字以内に収める
const MAX_MSG = 120;
const rawMsg = (process.env.COMMIT_MESSAGE || "（メッセージなし）").trim();
// コミットメッセージの1行目だけ使う（詳細説明は省略）
const firstLine = rawMsg.split("\n")[0];
const message =
  firstLine.length > MAX_MSG ? firstLine.slice(0, MAX_MSG) + "…" : firstLine;

const url = process.env.COMMIT_URL || "";
const author = process.env.COMMIT_AUTHOR || "";

const tweet = [
  `⚔️ The Four Primitives & Weapons 更新 [1.20.1]`,
  ``,
  message,
  author ? `👤 ${author}` : "",
  ``,
  url,
  ``,
  `#Minecraft #MinecraftMod #Forge #DrowseLab`,
]
  .filter((line) => line !== null)
  .join("\n")
  .trim();

(async () => {
  try {
    const { data } = await client.v2.tweet(tweet);
    console.log(`✅ Tweeted successfully (id: ${data.id})`);
    console.log("--- tweet content ---");
    console.log(tweet);
  } catch (err) {
    console.error("❌ Tweet failed:", err?.data ?? err.message);
    process.exit(1);
  }
})();
