import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import DashboardLayout from "../layouts/DashboardLayout";
import LoadingIndicator from "../components/LoadingIndicator";
import ErrorAlert from "../components/ErrorAlert";
import { listPaymentDetails } from "../api/paymentDetails";
import { listPurchasesByCustomer, updatePurchase, listPurchasesByCustomerPending } from "../api/purchases";
import { createInvoice, updateInvoice } from "../api/invoices";

// format de secours pour éviter tout crash si util manquant
const safeCurrency = (n) =>
  new Intl.NumberFormat("fr-FR", { style: "currency", currency: "EUR" }).format(Number(n || 0));

function useQuery() {
  const { search } = useLocation();
  return useMemo(() => new URLSearchParams(search), [search]);
}

export default function InvoiceCreateAssignPage() {
  const query = useQuery();
  const navigate = useNavigate();
  const initialCustomerId = query.get("customerId") || "";
  const lockedCustomerId = !!initialCustomerId; // 🔒 si vient de la page précédente, non modifiable

  const [customerId, setCustomerId] = useState(initialCustomerId);
  const [paymentDetailsId, setPaymentDetailsId] = useState("");
  const [paymentDetails, setPaymentDetails] = useState([]);
  const [invoiceId, setInvoiceId] = useState(null);

  const [loadingPD, setLoadingPD] = useState(false);
  const [loadingPurchases, setLoadingPurchases] = useState(false);
  const [purchases, setPurchases] = useState([]);
  const [error, setError] = useState(null);

  // Réinitialiser la facture et la liste si le client change
  useEffect(() => {
    setInvoiceId(null);
    setPurchases([]);
  }, [customerId]);

  // Charger les moyens de paiement
  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        setLoadingPD(true);
        setError(null);
        const pd = await listPaymentDetails();
        if (!mounted) return;
        setPaymentDetails(Array.isArray(pd) ? pd : []);
      } catch (e) {
        console.error("listPaymentDetails failed:", e);
        setError(e);
        setPaymentDetails([]);
      } finally {
        if (mounted) setLoadingPD(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

  // Créer la facture puis charger les achats du client
  async function handleCreateInvoice(e) {
    e.preventDefault();
    const cid = Number(customerId);
    const pdid = Number(paymentDetailsId);
    if (!cid || !pdid) {
      alert("Client et moyen de paiement sont requis.");
      return;
    }
    try {
      setLoadingPurchases(true);
      const inv = await createInvoice({ customerId: cid, paymentDetailsId: pdid });
      const newId = inv?.id ?? null;
      setInvoiceId(newId);

      const list = await listPurchasesByCustomerPending(cid);
      setPurchases(Array.isArray(list) ? list : []);
    } catch (e) {
      console.error("createInvoice/listPurchasesByCustomer failed:", e);
      alert(e?.response?.data?.message || e?.message || "Création de facture impossible");
      setInvoiceId(null);
      setPurchases([]);
    } finally {
      setLoadingPurchases(false);
    }
  }

  // ⚠️ PUT /api/purchase attend TOUT le payload -> on envoie l'objet complet + invoiceId mis à jour
  async function toggleAttach(purchase, checked) {
    if (!invoiceId) {
      alert("Créez la facture avant d'affecter des achats.");
      return;
    }
    const payload = {
      ...purchase,
      invoiceId: checked ? invoiceId : null,
    };
    try {
      await updatePurchase(payload);
      setPurchases((prev) =>
        prev.map((x) => (x.id === purchase.id ? { ...x, invoiceId: payload.invoiceId } : x))
      );
    } catch (e) {
      console.error("updatePurchase failed:", e);
      alert(e?.response?.data?.message || e?.message || "Mise à jour de l'achat impossible");
    }
  }

  // Valider la facture via PUT /api/invoices { id, isConfirmed: true }
  async function handleConfirmInvoice() {
    if (!invoiceId) {
      alert("Créez la facture avant de la valider.");
      return;
    }
    try {
      await updateInvoice(invoiceId,{ isConfirmed: true });
      navigate("/purchases");
    } catch (e) {
      console.error("updateInvoice failed:", e);
      alert(e?.response?.data?.message || e?.message || "Validation impossible");
    }
  }

  return (
    <DashboardLayout>
      {/* Header */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl sm:text-3xl font-bold">Créer & affecter une facture</h1>
        <button className="btn btn-ghost w-full sm:w-auto" onClick={() => navigate(-1)}>
          Retour
        </button>
      </div>

      <div className="mt-6 grid gap-6 md:grid-cols-3">
        {/* Formulaire facture */}
        <div className="card bg-base-100 shadow md:col-span-1">
          <div className="card-body">
            <h2 className="card-title">Informations facture</h2>
            <form className="space-y-3" onSubmit={handleCreateInvoice} noValidate>
              <label className="form-control">
                <span className="label-text">ID client</span>
                <input
                  type="number"
                  min="1"
                  step="1"
                  inputMode="numeric"
                  className="input input-bordered"
                  value={customerId}
                  onChange={(e) => setCustomerId(e.target.value)}
                  required
                  disabled={lockedCustomerId} // 🔒 non modifiable si transmis depuis la page précédente
                />
                {lockedCustomerId && (
                  <span className="mt-1 text-xs opacity-70">
                    ID verrouillé (transmis depuis la page précédente)
                  </span>
                )}
              </label>

              <label className="form-control">
                <span className="label-text">Moyen de paiement</span>
                <select
                  className="select select-bordered"
                  value={paymentDetailsId}
                  onChange={(e) => setPaymentDetailsId(e.target.value)}
                  required
                  disabled={loadingPD}
                >
                  <option value="">Choisir…</option>
                  {(paymentDetails || []).map((pd) => (
                    <option key={pd?.id} value={pd?.id}>
                      {pd?.paymentName ?? "Sans nom"} — {pd?.paymentTerm ?? "-"}
                    </option>
                  ))}
                </select>
              </label>

              <button
                type="submit"
                className="btn btn-primary w-full"
                disabled={loadingPD || !customerId || !paymentDetailsId}
              >
                {invoiceId ? "Recharger achats" : "Créer la facture"}
              </button>

              {invoiceId && (
                <div className="alert alert-info text-sm mt-2">
                  Facture courante&nbsp;: <b>#{invoiceId}</b>
                </div>
              )}
            </form>
          </div>
        </div>

        {/* Achats du client */}
        <div className="md:col-span-2">
          {loadingPurchases && <LoadingIndicator />}
          {error && <ErrorAlert error={error} />}

          {invoiceId && !loadingPurchases && (
            <div className="card bg-base-100 shadow">
              <div className="card-body">
                <div className="flex items-center justify-between">
                  <h3 className="card-title">Achats du client</h3>
                  <button className="btn btn-success btn-sm" onClick={handleConfirmInvoice}>
                    Valider la facture
                  </button>
                </div>

                {Array.isArray(purchases) && purchases.length > 0 ? (
                  <div className="overflow-x-auto mt-3">
                    <table className="table">
                      <thead>
                        <tr>
                          <th>Affecter</th>
                          <th>ID</th>
                          <th>Produit</th>
                          <th>Qté</th>
                          <th>Total HT</th>
                          <th>TVA</th>
                          <th>Total TVA</th>
                        </tr>
                      </thead>
                      <tbody>
                        {purchases.map((p) => (
                          <tr key={p?.id}>
                            <td>
                              <input
                                type="checkbox"
                                className="checkbox"
                                checked={!!p?.invoiceId && p?.invoiceId === invoiceId}
                                onChange={(e) => toggleAttach(p, e.target.checked)}
                              />
                            </td>
                            <td>{p?.id}</td>
                            <td>{p?.name ?? "-"}</td>
                            <td>{p?.quantity ?? 0}</td>
                            <td>{safeCurrency(p?.totalHT)}</td>
                            <td>{safeCurrency(p?.tvaApplied)}</td>
                            <td>{safeCurrency(p?.totalTva)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="p-6 text-center text-sm opacity-70">Aucun achat à afficher.</div>
                )}
              </div>
            </div>
          )}

          {!invoiceId && (
            <div className="p-6 text-center text-sm opacity-70">
              Crée d’abord la facture pour charger la liste des achats du client.
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
