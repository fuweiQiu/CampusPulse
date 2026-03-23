import { useEffect, useState } from "react";
import {
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import client from "../api/client";
import MetricCard from "../components/MetricCard";
import SectionTitle from "../components/SectionTitle";
import { useAuth } from "../context/AuthContext";

const pieColors = ["#ff7a59", "#2c8d8a", "#efb33f", "#64748b"];

export default function DashboardPage() {
  const { token } = useAuth();
  const [metrics, setMetrics] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [bundleLoading, setBundleLoading] = useState(false);

  useEffect(() => {
    const loadMetrics = async () => {
      setLoading(true);
      setError("");

      try {
        const { data } = await client.get("/api/metrics", { params: { token } });
        setMetrics(data);
      } catch (requestError) {
        setError(requestError.response?.data?.message || "目前無法讀取儀表板資料。");
      } finally {
        setLoading(false);
      }
    };

    loadMetrics();
  }, [token]);

  const emotionData = metrics
    ? Object.entries(metrics.emotionDistribution || {}).map(([name, value]) => ({ name, value }))
    : [];

  const downloadFhirBundle = async () => {
    setBundleLoading(true);
    setError("");

    try {
      const { data } = await client.get("/api/fhir/bundle", { params: { token } });
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/fhir+json" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "campuspulse-fhir-bundle.json";
      link.click();
      URL.revokeObjectURL(url);
    } catch (requestError) {
      setError(requestError.response?.data?.message || "目前無法匯出 FHIR Bundle。");
    } finally {
      setBundleLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <section className="panel rounded-[30px] p-6 md:p-8">
        <SectionTitle
          eyebrow="Weekly Pulse"
          title="近 7 天健康指標趨勢"
          description="從 FHIR Observation 聚合出平均壓力、睡眠時數與情緒分佈。未提供的欄位不採計，不再用假資料補值。"
        />

        {error ? (
          <div className="rounded-2xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
            {error}
          </div>
        ) : null}

        <div className="mb-5 flex flex-wrap gap-3">
          <button
            type="button"
            onClick={downloadFhirBundle}
            disabled={bundleLoading}
            className="rounded-full bg-slate-900 px-5 py-3 text-sm font-bold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
          >
            {bundleLoading ? "匯出中..." : "下載 FHIR Bundle"}
          </button>
          <div className="rounded-full border border-slate-200 bg-white px-4 py-3 text-sm text-slate-600">
            平均值只採計有明確數值的 Observation component
          </div>
        </div>

        <div className="grid gap-4 md:grid-cols-3">
          <MetricCard
            label="平均壓力"
            value={loading ? "-" : `${metrics?.averageStressScore ?? 0}/10`}
            hint="取近 7 天所有打卡的平均值"
            accent="#ff7a59"
          />
          <MetricCard
            label="平均睡眠"
            value={loading ? "-" : `${metrics?.averageSleepHours ?? 0}h`}
            hint="從訊息中推估的睡眠時數"
            accent="#2c8d8a"
          />
          <MetricCard
            label="總打卡次數"
            value={loading ? "-" : metrics?.totalCheckIns ?? 0}
            hint="越穩定記錄，圖表越有參考價值"
            accent="#efb33f"
          />
        </div>
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.25fr_0.75fr]">
        <div className="panel chart-card rounded-[30px] p-6 md:p-8">
          <SectionTitle
            eyebrow="Trend Lines"
            title="壓力與睡眠折線圖"
            description="若某天沒有完整數值，圖表會顯示 0 代表資料空白，而不是系統推算。"
          />

          <div className="h-[340px]">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={metrics?.dailyMetrics || []}>
                <CartesianGrid strokeDasharray="4 4" />
                <XAxis dataKey="date" tick={{ fill: "#64748b", fontSize: 12 }} />
                <YAxis tick={{ fill: "#64748b", fontSize: 12 }} />
                <Tooltip />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="averageStressScore"
                  name="平均壓力"
                  stroke="#ff7a59"
                  strokeWidth={3}
                  dot={{ r: 4 }}
                />
                <Line
                  type="monotone"
                  dataKey="averageSleepHours"
                  name="平均睡眠"
                  stroke="#2c8d8a"
                  strokeWidth={3}
                  dot={{ r: 4 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="panel chart-card rounded-[30px] p-6 md:p-8">
          <SectionTitle
            eyebrow="Emotion Mix"
            title="情緒分佈"
            description="看這週的主要情緒標籤落在哪些區間。"
          />

          <div className="h-[340px]">
            {emotionData.length ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={emotionData} dataKey="value" nameKey="name" outerRadius={110} innerRadius={60}>
                    {emotionData.map((entry, index) => (
                      <Cell key={entry.name} fill={pieColors[index % pieColors.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="flex h-full items-center justify-center rounded-[24px] border border-dashed border-slate-300 bg-white/50 px-6 text-center text-sm leading-7 text-slate-500">
                還沒有足夠的紀錄來計算情緒分佈。先去聊天打卡頁送出幾則近況。
              </div>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}
