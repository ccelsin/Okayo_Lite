import DashboardLayout from "../layouts/DashboardLayout";
import LoadingIndicator from "../components/LoadingIndicator";
import ErrorAlert from "../components/ErrorAlert";
import { useAsync } from "../hooks/useAsync";
import { listMyPurchases } from "../api/purchases";
import { formatCurrency } from "../utils/format";

export default function MyPurchasesPage() {
  const { data, isLoading, error, refresh } = useAsync(listMyPurchases, []);

  return (
    <DashboardLayout>
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">Mes achats</h1>
        <button className="btn btn-outline" onClick={refresh}>
          Rafraîchir
        </button>
      </div>
      <div className="mt-6">
        {isLoading && <LoadingIndicator />}
        <ErrorAlert error={error} />
        {data && data.length === 0 && <p>Aucun achat pour le moment.</p>}
        {data && data.length > 0 && (
          <div className="overflow-x-auto">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Produit</th>
                  <th>Quantité</th>
                  <th>Total HT</th>
                  <th>Total TVA</th>
                  <th>Statut</th>
                </tr>
              </thead>
              <tbody>
                {data.map((purchase) => (
                  <tr key={purchase.id}>
                    <td>{purchase.id}</td>
                    <td>{purchase.name}</td>
                    <td>{purchase.quantity}</td>
                    <td>{formatCurrency(purchase.totalHT)}</td>
                    <td>{formatCurrency(purchase.totalTva)}</td>
                    <td>
                      <span className={`badge ${purchase.isConfirmed ? "badge-success" : "badge-warning"}`}>
                        {purchase.isConfirmed ? "Confirmé" : "En attente"}
                      </span>
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
