import DashboardLayout from "../layouts/DashboardLayout";
import LoadingIndicator from "../components/LoadingIndicator";
import ErrorAlert from "../components/ErrorAlert";
import { useAsync } from "../hooks/useAsync";
import { listMyPurchases } from "../api/purchases";
import { formatCurrency as fmt } from "../utils/format";

export default function MyPurchasesPage() {
  const { data, isLoading, error, refresh } = useAsync(listMyPurchases, []);

  const purchases = Array.isArray(data) ? data : [];

  return (
    <DashboardLayout>
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl sm:text-3xl font-bold">Mes achats</h1>
        <button className="btn btn-outline w-full sm:w-auto" onClick={refresh} disabled={isLoading}>
          {isLoading ? "Chargement..." : "Rafraîchir"}
        </button>
      </div>

      <div className="mt-6">
        {isLoading && <LoadingIndicator />}
        <ErrorAlert error={error} />

        {/* État vide */}
        {!isLoading && !error && purchases.length === 0 && (
          <div className="rounded-2xl border border-slate-200 bg-white p-6 text-center text-slate-600">
            Aucun achat pour le moment.
          </div>
        )}

        {/* Cartes (mobile) */}
        {!isLoading && !error && purchases.length > 0 && (
          <ul className="md:hidden space-y-3">
            {purchases.map((p) => (
              <li key={p.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="text-xs text-slate-500">#ID</div>
                    <div className="text-base font-semibold text-slate-900">{p.id}</div>
                  </div>
                  <StatusBadge confirmed={p.isConfirmed} />
                </div>

                <div className="mt-3 space-y-1">
                  <Row label="Produit" value={p.name || "—"} />
                  <Row label="Quantité" value={String(p.quantity ?? 0)} />
                  <Row label="Total HT" value={fmtSafe(p.totalHT)} />
                  <Row label="Total TVA" value={fmtSafe(p.totalTva)} />
                </div>
              </li>
            ))}
          </ul>
        )}

        {/* Tableau (desktop) */}
        {!isLoading && !error && purchases.length > 0 && (
          <div className="hidden md:block overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <table className="min-w-full table">
              <thead className="bg-slate-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-600">
                    ID
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-600">
                    Produit
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-600">
                    Quantité
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-semibold uppercase tracking-wider text-slate-600">
                    Total HT
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-semibold uppercase tracking-wider text-slate-600">
                    Total TVA
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-semibold uppercase tracking-wider text-slate-600">
                    Statut
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {purchases.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50/60">
                    <td className="px-6 py-4 text-sm font-medium text-slate-900">{p.id}</td>
                    <td className="px-6 py-4 text-sm text-slate-800">{p.name || "—"}</td>
                    <td className="px-6 py-4 text-sm text-slate-800">{p.quantity ?? 0}</td>
                    <td className="px-6 py-4 text-right text-sm font-medium text-slate-900">
                      {fmtSafe(p.totalHT)}
                    </td>
                    <td className="px-6 py-4 text-right text-sm font-medium text-slate-900">
                      {fmtSafe(p.totalTva)}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <StatusBadge confirmed={p.isConfirmed} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}

/* ---- Petits composants ---- */

function Row({ label, value }) {
  return (
    <div className="flex items-center justify-between gap-4 text-sm">
      <span className="text-slate-500">{label}</span>
      <span className="font-medium text-slate-900">{value}</span>
    </div>
  );
}

function StatusBadge({ confirmed }) {
  // classes Tailwind/Daisy propres et visibles
  const base =
    "inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ring-1 ring-inset";
  return confirmed ? (
    <span className={`${base} bg-emerald-50 text-emerald-700 ring-emerald-200`}>
      Confirmé
    </span>
  ) : (
    <span className={`${base} bg-amber-50 text-amber-700 ring-amber-200`}>
      En attente
    </span>
  );
}

/* ---- Helpers ---- */
function fmtSafe(n) {
  try {
    return fmt(n);
  } catch {
    const v = Number(n || 0);
    return new Intl.NumberFormat(navigator.language || "fr-FR", {
      style: "currency",
      currency: "EUR",
    }).format(v);
  }
}
