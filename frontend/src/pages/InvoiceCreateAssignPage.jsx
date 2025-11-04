import { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import DashboardLayout from "../layouts/DashboardLayout";
import LoadingIndicator from "../components/LoadingIndicator";
import ErrorAlert from "../components/ErrorAlert";
import { listPaymentDetails } from "../api/paymentDetails";
import { listPurchasesByCustomerPending, updatePurchase } from "../api/purchases";
import { createInvoice, updateInvoice } from "../api/invoices";

const safeCurrency = (n) =>
  new Intl.NumberFormat("fr-FR", { style: "currency", currency: "EUR" }).format(Number(n || 0));

/** Si p.tvaApplied est un TAUX (ex: 20 => 20%) */
const safePercent = (n) =>
  new Intl.NumberFormat("fr-FR", {
    style: "percent",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(n || 0) / 100);

function useQuery() {
  const { search } = useLocation();
  return useMemo(() => new URLSearchParams(search), [search]);
}

export default function InvoiceCreateAssignPage() {
  const query = useQuery();
  const navigate = useNavigate();
  const initialCustomerId = query.get("customerId") || "";
  const lockedCustomerId = !!initialCustomerId;

  const [customerId, setCustomerId] = useState(initialCustomerId);
  const [paymentDetailsId, setPaymentDetailsId] = useState("");
  const [paymentDetails, setPaymentDetails] = useState([]);
  const [invoiceId, setInvoiceId] = useState(null);
  const [billingDate, setBillingDate] = useState("");
  const [dueDate, setDueDate] = useState("");

  const [loadingPD, setLoadingPD] = useState(false);
  const [loadingPurchases, setLoadingPurchases] = useState(false);
  const [purchases, setPurchases] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    setInvoiceId(null);
    setPurchases([]);
  }, [customerId]);

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        setLoadingPD(true);
        const pd = await listPaymentDetails();
        if (!mounted) return;
        setPaymentDetails(Array.isArray(pd) ? pd : []);
      } catch (e) {
        console.error("listPaymentDetails failed:", e);
        setError(e);
      } finally {
        if (mounted) setLoadingPD(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

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

      const payload = {
        customerId: cid,
        paymentDetailsId: pdid,
        ...(billingDate ? { billingDate } : {}),
        ...(dueDate ? { dueDate } : {}),
      };

      const inv = await createInvoice(payload);
      const newId = inv?.id ?? null;
      setInvoiceId(newId);

      const list = await listPurchasesByCustomerPending(cid);
      setPurchases(Array.isArray(list) ? list : []);
    } catch (e) {
      console.error("createInvoice failed:", e);
      alert(e?.response?.data?.message || e?.message || "Création de facture impossible");
      setInvoiceId(null);
      setPurchases([]);
    } finally {
      setLoadingPurchases(false);
    }
  }

  async function toggleAttach(purchase, checked) {
    if (!invoiceId) {
      alert("Créez la facture avant d'affecter des achats.");
      return;
    }
    const payload = { ...purchase, invoiceId: checked ? invoiceId : null };
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

  async function handleConfirmInvoice() {
    if (!invoiceId) {
      alert("Créez la facture avant de la valider.");
      return;
    }
    try {
      await updateInvoice(invoiceId, { isConfirmed: true });
      navigate("/purchases");
    } catch (e) {
      console.error("updateInvoice failed:", e);
      alert(e?.response?.data?.message || e?.message || "Validation impossible");
    }
  }

  return (
    <DashboardLayout>
      {/* Header mobile-first */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl sm:text-3xl font-bold">Créer & affecter une facture</h1>
        <button className="btn btn-ghost w-full sm:w-auto" onClick={() => navigate(-1)} type="button">
          Retour
        </button>
      </div>

      {/* Grille mobile-first : 1 colonne → desktop asymétrique */}
      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-[22rem,1fr]">
        {/* Colonne gauche : infos facture */}
        <div className="card bg-base-100 shadow">
          <div className="card-body">
            <h2 className="card-title">Informations facture</h2>
            <form className="space-y-3" onSubmit={handleCreateInvoice} noValidate>
              <label className="form-control">
                <span className="label-text">ID client</span>
                <input
                  type="number"
                  min="1"
                  step="1"
                  className="input input-bordered h-11 w-full"
                  value={customerId}
                  onChange={(e) => setCustomerId(e.target.value)}
                  required
                  disabled={lockedCustomerId}
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
                  className="select select-bordered h-11 w-full"
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

              {/* Dates (facultatives) */}
              <label className="form-control">
                <span className="label-text">Date de facturation</span>
                <input
                  type="date"
                  className="input input-bordered h-11 w-full"
                  value={billingDate}
                  onChange={(e) => setBillingDate(e.target.value)}
                />
              </label>

              <label className="form-control">
                <span className="label-text">Date d’échéance</span>
                <input
                  type="date"
                  className="input input-bordered h-11 w-full"
                  value={dueDate}
                  onChange={(e) => setDueDate(e.target.value)}
                />
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
                  Facture courante : <b>#{invoiceId}</b>
                </div>
              )}
            </form>
          </div>
        </div>

        {/* Colonne droite : achats */}
        <div>
          {loadingPurchases && <LoadingIndicator />}
          {error && <ErrorAlert error={error} />}

          {invoiceId && !loadingPurchases && (
            <div className="card bg-base-100 shadow">
              <div className="card-body">
                <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                  <h3 className="card-title">Achats du client</h3>
                  <button className="btn btn-success btn-sm w-full sm:w-auto" onClick={handleConfirmInvoice} type="button">
                    Valider la facture
                  </button>
                </div>

                {Array.isArray(purchases) && purchases.length > 0 ? (
                  <div className="overflow-x-auto mt-3">
                    <table className="table">
                      <thead className="text-sm">
                        <tr>
                          <th>Affecter</th>
                          <th>ID</th>
                          <th>Produit</th>
                          <th className="text-right whitespace-nowrap">Qté</th>
                          <th className="text-right whitespace-nowrap">Total HT</th>
                          <th className="text-right whitespace-nowrap">TVA</th>
                          <th className="text-right whitespace-nowrap">Total TVA</th>
                        </tr>
                      </thead>
                      <tbody className="[&>tr>td]:align-middle text-sm">
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
                            <td className="whitespace-nowrap">{p?.id}</td>
                            <td className="min-w-[14rem]">{p?.name ?? "-"}</td>
                            <td className="text-right whitespace-nowrap">{p?.quantity ?? 0}</td>
                            <td className="text-right whitespace-nowrap">{safeCurrency(p?.totalHT)}</td>
                            <td className="text-right whitespace-nowrap">{safePercent(p?.tvaApplied)}</td>
                            <td className="text-right whitespace-nowrap">{safeCurrency(p?.totalTva)}</td>
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
