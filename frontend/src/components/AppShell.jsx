import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

const navItems = [
  { to: "/chat", label: "聊天打卡" },
  { to: "/dashboard", label: "儀表板" },
  { to: "/community", label: "匿名社群" },
];

export default function AppShell() {
  const { username, displayName, patientFhirId, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/auth");
  };

  return (
    <div className="min-h-screen px-4 py-6 md:px-8 lg:px-12">
      <div className="mx-auto max-w-7xl">
        <header className="panel mb-8 rounded-[28px] px-6 py-5">
          <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
            <div>
              <p className="text-sm font-semibold uppercase tracking-[0.28em] text-[var(--teal)]">
                CampusPulse
              </p>
              <h1 className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900 md:text-4xl">
                為學生設計的健康節奏儀表板
              </h1>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-[var(--muted)] md:text-base">
                以聊天式打卡紀錄壓力、睡眠與情緒，快速看到近 7 天趨勢，並在匿名社群裡找到支持。
              </p>
            </div>
            <div className="rounded-3xl bg-white/70 px-4 py-3 text-sm shadow-sm">
              <p className="text-[var(--muted)]">目前登入</p>
              <p className="mt-1 text-lg font-bold text-slate-900">{displayName || username}</p>
              {patientFhirId ? (
                <p className="mt-1 text-xs text-slate-500">Patient/{patientFhirId}</p>
              ) : null}
            </div>
          </div>

          <div className="mt-6 flex flex-col gap-4 border-t border-white/70 pt-5 md:flex-row md:items-center md:justify-between">
            <nav className="flex flex-wrap gap-3">
              {navItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `rounded-full px-4 py-2 text-sm font-semibold transition ${
                      isActive
                        ? "bg-[var(--accent)] text-white shadow-lg shadow-orange-200"
                        : "bg-white/70 text-slate-700 hover:bg-white"
                    }`
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
            <button
              type="button"
              onClick={handleLogout}
              className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
            >
              登出
            </button>
          </div>
        </header>

        <main>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
