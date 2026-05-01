import { useEffect, useRef, useState } from "react";
import client from "../api/client";
import SectionTitle from "../components/SectionTitle";
import { useAuth } from "../context/AuthContext";
import { formatDateTime } from "../utils/format";

const starterPrompts = [
  "這週報告跟考試一起來，現在壓力大概 8 分。",
  "昨天只睡 4.5 小時，今天有點焦慮也很累。",
  "今天心情比較平靜，壓力大概 3 分。",
];
// const starterPrompt1 = "這週報告跟考試一起來，現在壓力大概 8 分。";
// const starterPrompt2 = "昨天只睡 4.5 小時，今天有點焦慮也很累。";
// const starterPrompt3 = "今天心情比較平靜，壓力大概 3 分。"

const pendingFieldLabels = {
  STRESS_SCORE: "等待補充壓力分數",
  SLEEP_HOURS: "等待補充睡眠時數",
  EMOTION: "等待補充情緒標籤",
};

export default function ChatPage() {
  const { token } = useAuth();
  const [conversation, setConversation] = useState(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(true);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    const loadHistory = async () => {
      setHistoryLoading(true);
      setError("");

      try {
        const { data } = await client.get("/api/chat/history", { params: { token } });
        setConversation(data);
      } catch (requestError) {
        setError(requestError.response?.data?.message || "目前無法讀取聊天紀錄。");
      } finally {
        setHistoryLoading(false);
      }
    };

    loadHistory();
  }, [token]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: historyLoading ? "auto" : "smooth", block: "end" });
  }, [historyLoading, conversation?.messages]);

  const submitMessage = async (event) => {
    event.preventDefault();
    if (!message.trim()) {
      return;
    }

    setLoading(true);
    setError("");

    try {
      const { data } = await client.post("/api/chat", { token, message: message.trim() });
      setConversation(data);
      setMessage("");
    } catch (requestError) {
      setError(requestError.response?.data?.message || "目前無法送出訊息。");
    } finally {
      setLoading(false);
    }
  };

  const messages = conversation?.messages?.length
    ? conversation.messages
    : [
        {
          id: "welcome",
          sender: "bot",
          content:
            "嗨，我在這裡陪你整理今天的狀態。你可以直接用聊天的方式告訴我壓力、睡眠或情緒，如果資訊還差一點，我會慢慢問完，不會自己亂補資料。",
          timestamp: new Date().toISOString(),
        },
      ];

  return (
    <div className="grid items-start gap-6 2xl:grid-cols-[minmax(0,1.08fr)_minmax(0,0.92fr)]">
      <section className="panel min-w-0 flex h-[72svh] min-h-[560px] max-h-[860px] flex-col overflow-hidden rounded-[30px] p-0 sm:h-[74svh]">
        <div className="border-b border-white/70 px-6 py-6 md:px-8">
          <SectionTitle
            eyebrow="Conversational Intake"
            title="多輪聊天式健康紀錄"
            description="這個版本不再捏造睡眠時數。若資訊不足，機器人會追問，直到能建立較接近 FHIR 的 Observation，或將欄位保留 unknown。"
          />
          {conversation?.awaitingFollowUp ? (
            <div className="inline-flex rounded-full bg-amber-100 px-4 py-2 text-xs font-bold tracking-[0.22em] text-amber-700">
              {pendingFieldLabels[conversation.pendingField] || "等待補充欄位"}
            </div>
          ) : null}
        </div>

        <div className="scroll-panel flex-1 space-y-4 overflow-y-auto px-4 py-5 sm:px-6 md:px-8">
          {historyLoading ? (
            <div className="rounded-[24px] border border-dashed border-slate-300 bg-white/50 px-5 py-12 text-center text-sm text-slate-500">
              讀取聊天紀錄中...
            </div>
          ) : (
            messages.map((item) => (
              <article
                key={item.id}
                className={`max-w-[92%] rounded-[24px] px-4 py-4 shadow-sm sm:max-w-[88%] sm:px-5 ${
                  item.sender === "user"
                    ? "ml-auto bg-slate-900 text-white"
                    : "border border-slate-200 bg-white/80 text-slate-700"
                }`}
              >
                <p className="break-words text-sm leading-7">{item.content}</p>
                <p className={`mt-2 text-xs ${item.sender === "user" ? "text-white/70" : "text-slate-400"}`}>
                  {formatDateTime(item.timestamp)}
                </p>
              </article>
            ))
          )}
          <div ref={messagesEndRef} />
        </div>

        <div className="border-t border-white/70 px-4 py-4 sm:px-6 md:px-8">
          <div className="mb-4 flex flex-wrap gap-2">
            {starterPrompts.map((prompt, idx) => (
              <button
                key={prompt}
                type="button"
                onClick={() => setMessage(prompt)}
                className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-600 transition hover:border-slate-300 hover:text-slate-900"
              >
                套用範例{idx + 1}
              </button>
            ))}
          </div>

          {error ? (
            <div className="mb-4 rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
              {error}
            </div>
          ) : null}

          <form onSubmit={submitMessage} className="flex flex-col gap-3 lg:flex-row">
            <textarea
              value={message}
              onChange={(event) => setMessage(event.target.value)}
              placeholder="直接像聊天一樣輸入。若缺少睡眠時數或情緒，機器人會自動追問。"
              className="h-24 min-h-24 flex-1 resize-none rounded-[24px] border border-slate-200 bg-white/80 px-5 py-4 text-base leading-7 text-slate-800 outline-none transition focus:border-[var(--accent)]"
            />
            <button
              type="submit"
              disabled={loading}
              className="rounded-[24px] bg-[var(--accent)] px-6 py-4 text-sm font-bold text-white transition hover:bg-[var(--accent-deep)] disabled:cursor-not-allowed disabled:opacity-70 lg:self-end"
            >
              {loading ? "送出中..." : "傳送"}
            </button>
          </form>
        </div>
      </section>

      <section className="grid min-w-0 gap-6 2xl:h-[72svh] 2xl:min-h-[560px] 2xl:max-h-[860px] 2xl:grid-rows-[minmax(0,0.95fr)_minmax(0,1.05fr)]">
        <div className="panel min-w-0 flex min-h-[280px] flex-col overflow-hidden rounded-[30px] p-6 md:p-8">
          <SectionTitle
            eyebrow="Latest Observation"
            title="最近一次 FHIR 結果"
            description="完成多輪追問後，後端會輸出一筆 `Observation`，缺漏欄位則以 unknown 或空值處理，不會憑空填數字。"
          />

          {conversation?.latestObservation ? (
            <div className="mt-1 flex min-h-0 min-w-0 flex-1 flex-col gap-4">
              <div className="grid gap-4 sm:grid-cols-3">
                <ResultTile
                  label="壓力"
                  value={
                    conversation.latestObservation.stressScore === null
                      ? "unknown"
                      : `${conversation.latestObservation.stressScore}/10`
                  }
                  accent="#ff7a59"
                />
                <ResultTile
                  label="睡眠"
                  value={
                    conversation.latestObservation.sleepHours === null
                      ? "unknown"
                      : `${conversation.latestObservation.sleepHours}h`
                  }
                  accent="#2c8d8a"
                />
                <ResultTile
                  label="情緒"
                  value={conversation.latestObservation.emotionDisplay || "unknown"}
                  accent="#efb33f"
                />
              </div>

              <div className="scroll-panel min-h-0 min-w-0 flex-1 overflow-y-auto rounded-[24px] border border-slate-200 bg-white/80 p-5">
                <dl className="grid gap-3 text-sm text-slate-700">
                  <div className="grid gap-1 border-b border-slate-100 pb-2 sm:grid-cols-[140px_minmax(0,1fr)] sm:items-center sm:gap-4">
                    <dt>FHIR Observation ID</dt>
                    <dd className="break-all font-semibold">{conversation.latestObservation.fhirId}</dd>
                  </div>
                  <div className="grid gap-1 border-b border-slate-100 pb-2 sm:grid-cols-[140px_minmax(0,1fr)] sm:items-center sm:gap-4">
                    <dt>Status</dt>
                    <dd className="break-words font-semibold">{conversation.latestObservation.status}</dd>
                  </div>
                  <div className="grid gap-1 sm:grid-cols-[140px_minmax(0,1fr)] sm:items-center sm:gap-4">
                    <dt>Recorded At</dt>
                    <dd className="break-words font-semibold">
                      {formatDateTime(conversation.latestObservation.effectiveDateTime)}
                    </dd>
                  </div>
                  {conversation.latestObservation.resourceUrl ? (
                    <div className="grid gap-1 sm:grid-cols-[140px_minmax(0,1fr)] sm:items-center sm:gap-4">
                      <dt>Cloud Resource</dt>
                      <dd>
                        <a
                          href={conversation.latestObservation.resourceUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="break-all font-semibold text-[var(--teal)] underline decoration-transparent transition hover:decoration-inherit"
                        >
                          {conversation.latestObservation.resourceUrl}
                        </a>
                      </dd>
                    </div>
                  ) : null}
                </dl>
                <div className="mt-4 rounded-2xl bg-slate-50 px-4 py-4">
                  <p className="break-words text-sm leading-7 text-slate-600">
                    {conversation.latestObservation.suggestion}
                  </p>
                </div>
                {conversation.latestObservation.sourceText ? (
                  <details className="mt-4 rounded-2xl border border-slate-200 bg-white/70">
                    <summary className="cursor-pointer list-none px-4 py-3 text-sm font-semibold text-slate-700">
                      查看本次對話原文
                    </summary>
                    <div className="border-t border-slate-100 px-4 py-4">
                      <p className="whitespace-pre-wrap break-words text-sm leading-7 text-slate-600">
                        {conversation.latestObservation.sourceText}
                      </p>
                    </div>
                  </details>
                ) : null}
              </div>
            </div>
          ) : (
            <div className="rounded-[24px] border border-dashed border-slate-300 bg-white/50 px-5 py-12 text-center text-sm leading-7 text-slate-500">
              還沒有完成的 Observation。先開始對話，或回答機器人的追問。
            </div>
          )}
        </div>

        <div className="panel min-w-0 rounded-[30px] p-6 md:p-8">
          <SectionTitle
            eyebrow="FHIR JSON"
            title="最近一次資源預覽"
            description="這裡顯示實際輸出的 FHIR JSON。未提供的欄位不會被捏造，必要時會用 `dataAbsentReason`。"
          />

          <details className="mt-1 overflow-hidden rounded-[24px] border border-slate-200 bg-white/70" open>
            <summary className="cursor-pointer list-none px-5 py-4 text-sm font-semibold text-slate-700">
              展開 JSON 預覽
            </summary>
            <div className="border-t border-slate-200 p-3 sm:p-4">
              <pre className="scroll-panel max-h-[360px] overflow-auto whitespace-pre-wrap break-all rounded-[20px] bg-slate-950 px-4 py-4 text-xs leading-6 text-slate-100 sm:px-5">
                {JSON.stringify(conversation?.latestFhirObservation || {}, null, 2)}
              </pre>
            </div>
          </details>
        </div>
      </section>
    </div>
  );
}

function ResultTile({ label, value, accent }) {
  return (
    <div className="min-w-0 rounded-[24px] border border-slate-200 bg-white/80 p-4">
      <p className="text-sm font-semibold text-[var(--muted)]">{label}</p>
      <div className="mt-3 flex items-center gap-3">
        <span className="h-3 w-3 rounded-full" style={{ backgroundColor: accent }} />
        <p className="break-all text-2xl font-extrabold tracking-tight text-slate-900">{value}</p>
      </div>
    </div>
  );
}
