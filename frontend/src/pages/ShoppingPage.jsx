import { useState } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import LoadingIndicator from "../components/LoadingIndicator";
import ErrorAlert from "../components/ErrorAlert";
import { useAsync } from "../hooks/useAsync";
import { listProducts } from "../api/products";
import { createPurchase } from "../api/purchases";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.css";
import { formatCurrency } from "../utils/format";

export default function ShoppingPage() {
  const { data: products, isLoading, error, refresh } = useAsync(listProducts, []);
  // Quantités par produit: { [productId]: number }
  const [qty, setQty] = useState({});
  // Soumission en cours par produit (pour désactiver son bouton)
  const [submittingId, setSubmittingId] = useState(null);

  function setQuantity(productId, value) {
    // Autoriser vide temporaire, sinon forcer entier >= 1
    if (value === "") {
      setQty((q) => ({ ...q, [productId]: "" }));
      return;
    }
    const n = Number(value);
    if (Number.isNaN(n)) return;
    const clamped = Math.max(1, Math.floor(n));
    setQty((q) => ({ ...q, [productId]: clamped }));
  }

  async function handleBuy(product) {
    try {
      const quantity = qty[product.id] === "" || qty[product.id] == null ? 1 : Number(qty[product.id]);
      if (!Number.isInteger(quantity) || quantity < 1) {
        Swal.fire({ icon: "error", title: "Quantité invalide", text: "Veuillez saisir un entier ≥ 1." });
        return;
      }
      setSubmittingId(product.id);
      await createPurchase({ productId: product.id, quantity });
      Swal.fire({ icon: "success", title: "Achat créé", text: `${product.name} × ${quantity}`, timer: 1600, showConfirmButton: false });
      // Optionnel: remettre la quantité à 1 après achat
      setQty((q) => ({ ...q, [product.id]: 1 }));
      // Si tu veux rafraîchir la liste (pas nécessaire ici)
      // await refresh();
    } catch (e) {
      const apiMsg = e?.response?.data?.message || e?.message || "Impossible de créer l'achat";
      Swal.fire({ icon: "error", title: "Échec de l'achat", text: apiMsg });
    } finally {
      setSubmittingId(null);
    }
  }

  return (
    <DashboardLayout>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl sm:text-3xl font-bold">Boutique</h1>
        <div className="flex gap-2">
          <button className="btn btn-outline w-full sm:w-auto" onClick={refresh} disabled={isLoading}>
            {isLoading ? "Chargement..." : "Rafraîchir"}
          </button>
        </div>
      </div>

      <div className="mt-6">
        {isLoading && <LoadingIndicator />}
        <ErrorAlert error={error} />

        {Array.isArray(products) && products.length > 0 ? (
          <>
            {/* Mobile: cartes */}
            <div className="md:hidden space-y-3">
              {products.map((p) => (
                <div key={p.id} className="card bg-base-100 shadow">
                  <div className="card-body p-4">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <div className="text-xs opacity-60">ID</div>
                        <div className="font-medium">{p.id}</div>
                      </div>
                      <div className="text-right">
                        <div className="text-xs opacity-60">Prix HT</div>
                        <div className="font-medium">{formatCurrency(p.unitPriceHT)}</div>
                      </div>
                    </div>

                    <div className="mt-2">
                      <div className="text-sm font-medium">{p.name}</div>
                      <div className="text-xs opacity-60">TVA: {p.tvaId}</div>
                    </div>

                    <div className="mt-3 flex items-center gap-2">
                      <input
                        type="number"
                        min="1"
                        step="1"
                        inputMode="numeric"
                        className="input input-bordered w-24"
                        value={qty[p.id] ?? 1}
                        onChange={(e) => setQuantity(p.id, e.target.value)}
                      />
                      <button
                        className="btn btn-primary flex-1"
                        onClick={() => handleBuy(p)}
                        disabled={submittingId === p.id}
                      >
                        {submittingId === p.id ? "Ajout..." : "Acheter"}
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Desktop: tableau dans une card */}
            <div className="hidden md:block">
              <div className="card bg-base-100 shadow">
                <div className="card-body pb-0">
                  <h3 className="card-title">Produits</h3>
                </div>
                <div className="overflow-x-auto">
                  <table className="table table-zebra">
                    <thead className="sticky top-0 bg-base-100 z-10">
                      <tr>
                        <th className="w-16">ID</th>
                        <th className="min-w-[240px]">Nom</th>
                        <th className="w-40">Prix HT</th>
                        <th className="w-24">TVA</th>
                        <th className="w-40">Quantité</th>
                        <th className="w-40 text-right">Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {products.map((p) => (
                        <tr key={p.id} className="hover">
                          <td>{p.id}</td>
                          <td className="truncate" title={p.name || ""}>{p.name}</td>
                          <td className="whitespace-nowrap">{formatCurrency(p.unitPriceHT)}</td>
                          <td className="whitespace-nowrap">{p.tvaId}</td>
                          <td>
                            <input
                              type="number"
                              min="1"
                              step="1"
                              inputMode="numeric"
                              className="input input-bordered w-28"
                              value={qty[p.id] ?? 1}
                              onChange={(e) => setQuantity(p.id, e.target.value)}
                            />
                          </td>
                          <td className="text-right">
                            <button
                              className="btn btn-primary"
                              onClick={() => handleBuy(p)}
                              disabled={submittingId === p.id}
                            >
                              {submittingId === p.id ? "Ajout..." : "Acheter"}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </>
        ) : (
          !isLoading && <div className="p-6 text-center text-sm text-base-content/70">Aucun produit disponible.</div>
        )}
      </div>
    </DashboardLayout>
  );
}
