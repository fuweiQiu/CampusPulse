import { useEffect, useState } from "react";
import client from "../api/client";
import SectionTitle from "../components/SectionTitle";
import { useAuth } from "../context/AuthContext";
import { formatDateTime } from "../utils/format";

export default function CommunityPage() {
  const { token } = useAuth();
  const [content, setContent] = useState("");
  const [posts, setPosts] = useState([]);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const loadPosts = async () => {
    try {
      const { data } = await client.get("/api/community/posts");
      setPosts(data);
    } catch (requestError) {
      setError(requestError.response?.data?.message || "目前無法載入社群貼文。");
    }
  };

  useEffect(() => {
    loadPosts();
  }, []);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError("");

    try {
      await client.post("/api/community/posts", { token, content });
      setContent("");
      await loadPosts();
    } catch (requestError) {
      setError(requestError.response?.data?.message || "目前無法發布貼文。");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="grid gap-6 xl:grid-cols-[0.92fr_1.08fr]">
      <section className="panel rounded-[30px] p-6 md:p-8">
        <SectionTitle
          eyebrow="Anonymous Support"
          title="匿名支持社群"
          description="只顯示貼文內容與時間，不顯示使用者資訊。適合分享近況、壓力源或給彼此一句支持。"
        />

        <form onSubmit={handleSubmit} className="space-y-4">
          <textarea
            value={content}
            onChange={(event) => setContent(event.target.value)}
            placeholder="寫下一句想說的話。像是：這週期中考讓我很緊繃，希望大家也都能撐過去。"
            className="h-48 w-full rounded-[24px] border border-slate-200 bg-white/80 px-5 py-4 text-base leading-7 text-slate-800 outline-none transition focus:border-[var(--teal)]"
            required
          />

          {error ? (
            <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
              {error}
            </div>
          ) : null}

          <button
            type="submit"
            disabled={submitting}
            className="rounded-full bg-[var(--teal)] px-6 py-3 text-sm font-bold text-white transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-70"
          >
            {submitting ? "發送中..." : "發布匿名貼文"}
          </button>
        </form>
      </section>

      <section className="space-y-4">
        <SectionTitle
          eyebrow="Community Feed"
          title="最新貼文"
          description="這裡只保留內容與時間，避免暴露個人資訊。"
        />

        {posts.length ? (
          posts.map((post) => (
            <article key={post.id} className="panel rounded-[26px] p-5">
              <div className="flex items-center justify-between gap-4">
                <p className="text-sm font-bold uppercase tracking-[0.24em] text-[var(--accent-deep)]">
                  Post #{post.id}
                </p>
                <p className="text-sm text-[var(--muted)]">{formatDateTime(post.createdAt)}</p>
              </div>
              <p className="mt-4 text-sm leading-7 text-slate-700">{post.content}</p>
            </article>
          ))
        ) : (
          <div className="panel rounded-[26px] p-8 text-sm leading-7 text-slate-500">
            還沒有貼文。你可以先發一則，為這個匿名空間建立第一個聲音。
          </div>
        )}
      </section>
    </div>
  );
}
