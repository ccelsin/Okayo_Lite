import { useMemo } from "react";
import { useNavigate } from "react-router-dom";
import DashboardLayout from "../layouts/DashboardLayout";
import LoadingIndicator from "../components/LoadingIndicator";
import ErrorAlert from "../components/ErrorAlert";
import { useAsync } from "../hooks/useAsync";
import { listPurchases } from "../api/purchases";

const safeCurrency = (n) =>
  new Intl.NumberFormat("fr-FR", { style: "currency", currency: "EUR" }).format(Number(n || 0));

export default function PurchasesPage() {
  const navigate = useNavigate();
  const { data: raw, isLoading, error, refresh } = useAsync(listPurchases, []);
  const data = useMemo(() => (Array.isArray(raw) ? raw : []), [raw]);

  return (
    <DashboardLayout>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl sm:text-3xl font-bold">Achats</h1>
        <div className="flex flex-col sm:flex-row gap-2">
          <button className="btn btn-primary w-full sm:w-auto" onClick={() => navigate("/invoices/new")}>
            Facturer un client
          </button>
          <button className="btn btn-outline w-full sm:w-auto" onClick={refresh} disabled={isLoading}>
            {isLoading ? "Chargement..." : "Rafraîchir"}
          </button>
        </div>
      </div>

      <div className="mt-6">
        {isLoading && <LoadingIndicator />}
        <ErrorAlert error={error} />

        {data.length > 0 ? (
          <div className="card bg-base-100 shadow">
            <div className="overflow-x-auto">
              <table className="table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Produit</th>
                    <th>Quantité</th>
                    <th>Total HT</th>
                    <th>TVA</th>
                    <th>Total TVA</th>
                    <th>Acheteur</th>
                    <th>Confirmé</th>
                    <th className="text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {data.map((p) => (
                    <tr key={p?.id}>
                      <td>{p?.id}</td>
                      <td>{p?.name ?? "-"}</td>
                      <td>{p?.quantity ?? 0}</td>
                      <td>{safeCurrency(p?.totalHT)}</td>
                      <td>{safeCurrency(p?.tvaApplied)}</td>
                      <td>{safeCurrency(p?.totalTva)}</td>
                      <td>{p?.purchaserId ?? "-"}</td>
                      <td>
                        <span className={`badge ${p?.isConfirmed ? "badge-success" : "badge-warning"}`}>
                          {p?.isConfirmed ? "Oui" : "En attente"}
                        </span>
                      </td>
                      <td className="text-right">
                        <button
                          className="btn btn-xs btn-primary"
                          onClick={() =>
                            navigate(`/invoices/new?customerId=${encodeURIComponent(p?.purchaserId ?? "")}`)
                          }
                        >
                          Créer facture
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          !isLoading && <div className="p-6 text-center text-sm opacity-70">Aucun achat en attente.</div>
        )}
      </div>
    </DashboardLayout>
  );
}
