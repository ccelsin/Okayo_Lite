import DashboardLayout from "../../layouts/DashboardLayout";
import LoadingIndicator from "../../components/LoadingIndicator";
import ErrorAlert from "../../components/ErrorAlert";
import { useAsync } from "../../hooks/useAsync";
import { listPurchases } from "../../api/purchases";
import { formatCurrency } from "../../utils/format";

export default function PurchasesPage() {
  const { data, isLoading, error, refresh } = useAsync(listPurchases, []);

  return (
    <DashboardLayout>
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">Achats</h1>
        <button className="btn btn-outline" onClick={refresh}>
          Rafraîchir
        </button>
      </div>
      <div className="mt-6">
        {isLoading && <LoadingIndicator />}
        <ErrorAlert error={error} />
        {data && (
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
                </tr>
              </thead>
              <tbody>
                {data.map((purchase) => (
                  <tr key={purchase.id}>
                    <td>{purchase.id}</td>
                    <td>{purchase.name}</td>
                    <td>{purchase.quantity}</td>
                    <td>{formatCurrency(purchase.totalHT)}</td>
                    <td>{formatCurrency(purchase.tvaApplied)}</td>
                    <td>{formatCurrency(purchase.totalTva)}</td>
                    <td>{purchase.purchaserId}</td>
                    <td>
                      <span className={`badge ${purchase.isConfirmed ? "badge-success" : "badge-warning"}`}>
                        {purchase.isConfirmed ? "Oui" : "En attente"}
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
