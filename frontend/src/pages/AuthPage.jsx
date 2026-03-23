import { useState } from "react";
import { useNavigate } from "react-router-dom";
import client from "../api/client";
import { useAuth } from "../context/AuthContext";

const initialForm = {
  username: "",
  password: "",
  displayName: "",
  birthDate: "",
  gender: "unknown",
};

export default function AuthPage() {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const endpoint = mode === "login" ? "/api/login" : "/api/register";
      const payload =
        mode === "login"
          ? { username: form.username, password: form.password }
          : form;
      const { data } = await client.post(endpoint, payload);
      login(data);
      navigate("/chat");
    } catch (requestError) {
      setError(requestError.response?.data?.message || "目前無法完成驗證，請稍後再試。");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center px-4 py-10">
      <div className="grid w-full max-w-6xl gap-6 lg:grid-cols-[1.08fr_0.92fr]">
        <section className="panel relative overflow-hidden rounded-[36px] px-8 py-10 md:px-12">
          <div className="absolute right-0 top-0 h-44 w-44 rounded-full bg-[var(--accent)]/15 blur-3xl" />
          <div className="absolute bottom-0 left-0 h-52 w-52 rounded-full bg-[var(--teal)]/15 blur-3xl" />

          <p className="text-sm font-bold uppercase tracking-[0.32em] text-[var(--teal)]">
            CampusPulse
          </p>
          <h1 className="mt-4 max-w-xl text-4xl font-extrabold leading-tight tracking-tight text-slate-900 md:text-6xl">
            聊一句近況，換一張更看得懂自己的健康地圖。
          </h1>
          <p className="mt-6 max-w-2xl text-base leading-8 text-slate-600">
            給學生使用的示範型健康平台。登入後可以用自然語句記錄今天的壓力、睡眠和情緒，再用一週圖表看出節奏變化。
          </p>

          <div className="mt-10 grid gap-4 md:grid-cols-3">
            <FeatureCard title="聊天式打卡" description="輸入今天的身心狀態，自動換成結構化 Observation。" />
            <FeatureCard title="7 天趨勢" description="用視覺化圖表掌握壓力與睡眠節奏。" />
            <FeatureCard title="匿名支持" description="保留安全距離，也能在社群中看見他人的陪伴。" />
          </div>
        </section>

        <section className="panel rounded-[36px] px-7 py-8 md:px-10">
          <div className="inline-flex rounded-full bg-white/80 p-1">
            {["login", "register"].map((tab) => (
              <button
                key={tab}
                type="button"
                onClick={() => setMode(tab)}
                className={`rounded-full px-5 py-2 text-sm font-semibold transition ${
                  mode === tab
                    ? "bg-[var(--accent)] text-white"
                    : "text-slate-600 hover:text-slate-900"
                }`}
              >
                {tab === "login" ? "登入" : "註冊"}
              </button>
            ))}
          </div>

          <div className="mt-8">
            <h2 className="text-3xl font-extrabold tracking-tight text-slate-900">
              {mode === "login" ? "回到你的健康面板" : "建立你的 CampusPulse 帳號"}
            </h2>
            <p className="mt-2 text-sm leading-6 text-[var(--muted)]">
              {mode === "login"
                ? "登入後會取得 JWT token，前端會用它呼叫聊天、儀表板與社群 API。"
                : "註冊完成後會直接取得 token，方便立即開始使用示範功能。"}
            </p>
          </div>

          <form onSubmit={handleSubmit} className="mt-8 space-y-5">
            <label className="block">
              <span className="mb-2 block text-sm font-semibold text-slate-700">使用者名稱</span>
              <input
                type="text"
                value={form.username}
                onChange={(event) => setForm((current) => ({ ...current, username: event.target.value }))}
                className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                placeholder="例如 frank_student"
                required
              />
            </label>

            <label className="block">
              <span className="mb-2 block text-sm font-semibold text-slate-700">密碼</span>
              <input
                type="password"
                value={form.password}
                onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
                className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                placeholder="至少 6 碼"
                required
              />
            </label>

            {mode === "register" ? (
              <>
                <label className="block">
                  <span className="mb-2 block text-sm font-semibold text-slate-700">顯示名稱</span>
                  <input
                    type="text"
                    value={form.displayName}
                    onChange={(event) => setForm((current) => ({ ...current, displayName: event.target.value }))}
                    className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                    placeholder="可選填，會寫進 FHIR Patient.name"
                  />
                </label>

                <div className="grid gap-5 md:grid-cols-2">
                  <label className="block">
                    <span className="mb-2 block text-sm font-semibold text-slate-700">生日</span>
                    <input
                      type="date"
                      value={form.birthDate}
                      onChange={(event) => setForm((current) => ({ ...current, birthDate: event.target.value }))}
                      className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                    />
                  </label>

                  <label className="block">
                    <span className="mb-2 block text-sm font-semibold text-slate-700">性別</span>
                    <select
                      value={form.gender}
                      onChange={(event) => setForm((current) => ({ ...current, gender: event.target.value }))}
                      className="w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 outline-none transition focus:border-[var(--accent)]"
                    >
                      <option value="unknown">不指定</option>
                      <option value="male">Male</option>
                      <option value="female">Female</option>
                      <option value="other">Other</option>
                    </select>
                  </label>
                </div>
              </>
            ) : null}

            {error ? (
              <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
                {error}
              </div>
            ) : null}

            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-2xl bg-slate-900 px-5 py-3 text-sm font-bold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
            >
              {loading ? "處理中..." : mode === "login" ? "登入並開始打卡" : "建立帳號"}
            </button>
          </form>
        </section>
      </div>
    </div>
  );
}

function FeatureCard({ title, description }) {
  return (
    <div className="rounded-[24px] border border-white/70 bg-white/60 p-5 shadow-sm">
      <h3 className="text-lg font-bold text-slate-900">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-slate-600">{description}</p>
    </div>
  );
}
