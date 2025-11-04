import Topbar from "../components/Topbar";

export default function DashboardLayout({ children, variant = "default" }) {
  const isAuth = variant === "auth";

  return (
    <div
      className={
        isAuth
          ? "min-h-screen bg-base-200 flex flex-col"
          : "min-h-screen bg-base-200"
      }
    >
      <Topbar />
      <main
        className={
          isAuth
            ? "flex flex-1 items-center justify-center px-4 py-8"
            : "mx-auto max-w-6xl px-4 py-8"
        }
      >
        {children}
      </main>
    </div>
  );
}
