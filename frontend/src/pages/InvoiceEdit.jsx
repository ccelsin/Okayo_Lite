import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import DashboardLayout from "../layouts/DashboardLayout";
import LoadingIndicator from "../components/LoadingIndicator";
import ErrorAlert from "../components/ErrorAlert";

import { getInvoice, updateInvoice, downloadInvoicePdf } from "../api/invoices";
import { listPurchasesByCustomerPending, updatePurchase } from "../api/purchases";

const safeCurrency = (n) =>
  new Intl.NumberFormat("fr-FR", { style: "currency", currency: "EUR" }).format(Number(n || 0));

const fmtDateInput = (iso) => {
  if (!iso) return "";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  // normaliser pour <input type="date">
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
};

export default function InvoiceEdit() {
  const { id } = useParams(); // invoice id
  const invoiceId = Number(id);
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  // Champs facture
  const [reference, setReference] = useState("");
  const [customerId, setCustomerId] = useState(null);
  const [billingDate, setBillingDate] = useState("");
  const [dueDate, setDueDate] = useState("");
  const [isConfirmed, setIsConfirmed] = useState(false);

  // Purchases
  const [loadingPurchases, setLoadingPurchases] = useState(false);
  const [purchases, setPurchases] = useState([]);

  // 1) Charger la facture
  useEffect(() => {
    let active = true;
    (async () => {
      try {
        setLoading(true);
        setError("");
        const inv = await getInvoice(invoiceId);
        if (!active) return;

        setReference(inv?.reference || inv?.id || "");
        setCustomerId(inv?.customerId ?? inv?.customer?.id ?? null);
        setBillingDate(fmtDateInput(inv?.billingDate));
        setDueDate(fmtDateInput(inv?.dueDate));
        setIsConfirmed(Boolean(inv?.isConfirmed ?? inv?.confirmed));

      } catch (e) {
        console.error("[InvoiceEdit] getInvoice error:", e);
        if (active) setError(e?.message || "Chargement de la facture impossible");
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, [invoiceId]);

  // 2) Charger les achats du client (tous) pour pouvoir cocher/décocher
  useEffect(() => {
    if (!customerId) return;
    let active = true;
    (async () => {
      try {
        setLoadingPurchases(true);
        const list = await listPurchasesByCustomerPending(Number(customerId));
        if (!active) return;
        setPurchases(Array.isArray(list) ? list : []);
      } catch (e) {
        console.error("[InvoiceEdit] listPurchasesByCustomer error:", e);
        setError(e?.message || "Chargement des achats impossible");
        setPurchases([]);
      } finally {
        if (active) setLoadingPurchases(false);
      }
    })();
    return () => { active = false; };
  }, [customerId]);

  // 3) Attacher/détacher un achat à la facture
  const toggleAttach = async (purchase, checked) => {
    if (!invoiceId) {
      alert("Aucune facture chargée.");
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
  };

  // 4) Enregistrer les champs de la facture (dates + statut)
  const onSave = async (e) => {
    e.preventDefault();
    try {
      setSaving(true);
      const payload = {
        ...(billingDate ? { billingDate } : { billingDate: null }),
        ...(dueDate ? { dueDate } : { dueDate: null }),
        isConfirmed,
      };
      await updateInvoice(invoiceId, payload);
      navigate("/invoices");
    } catch (e) {
      console.error("updateInvoice failed:", e);
      alert(e?.response?.data?.message || e?.message || "Enregistrement impossible");
    } finally {
      setSaving(false);
    }
  };

  const onDownload = async () => {
    try {
      const blob = await downloadInvoicePdf(invoiceId);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${reference || invoiceId}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (e) {
      alert("Téléchargement impossible: " + (e?.message || "Erreur inconnue"));
    }
  };

  return (
    <DashboardLayout>
      <div className="min-h-screen bg-slate-50">
        <div className="mx-auto max-w-6xl px-4 py-6 md:py-10">
          {/* Header */}
          <div className="flex items-start justify-between gap-3 flex-wrap">
            <h1 className="text-2xl md:text-3xl font-bold text-slate-900">Modifier la facture</h1>
            <div className="flex gap-2">
              <button
                onClick={() => navigate(-1)}
                className="rounded-xl border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                Retour
              </button>
              <button
                onClick={onDownload}
                className="rounded-xl bg-indigo-600 px-3 py-2 text-sm font-medium text-white hover:bg-indigo-700"
              >
                Télécharger PDF
              </button>
            </div>
          </div>

          {/* Facture - infos & formulaire */}
          {loading ? (
            <div className="mt-4 rounded-2xl border border-slate-200 bg-white p-4 md:p-6 shadow-sm">
              Chargement…
            </div>
          ) : error ? (
            <div className="mt-4"><ErrorAlert error={error} /></div>
          ) : (
            <form onSubmit={onSave} className="mt-6 grid gap-6 md:grid-cols-3">
              {/* Colonne gauche : infos facture */}
              <div className="md:col-span-1 rounded-2xl border border-slate-200 bg-white p-4 md:p-6 shadow-sm space-y-4">
                <div>
                  <label className="block text-sm font-medium text-slate-600">Référence</label>
                  <input
                    type="text"
                    value={reference}
                    disabled
                    className="mt-1 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm"
                  />
                  <p className="mt-1 text-xs text-slate-500">
                    La référence est générée côté serveur et n’est pas modifiable.
                  </p>
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-600">Client ID</label>
                  <input
                    type="number"
                    value={customerId ?? ""}
                    disabled
                    className="mt-1 w-full rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-sm"
                  />
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-600">Date de facturation</label>
                    <input
                      type="date"
                      value={billingDate}
                      onChange={(e) => setBillingDate(e.target.value)}
                      className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-600">Date d’échéance</label>
                    <input
                      type="date"
                      value={dueDate}
                      onChange={(e) => setDueDate(e.target.value)}
                      className="mt-1 w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm"
                    />
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <input
                    id="isConfirmed"
                    type="checkbox"
                    checked={isConfirmed}
                    onChange={(e) => setIsConfirmed(e.target.checked)}
                    className="checkbox checkbox-sm"
                  />
                  <label htmlFor="isConfirmed" className="text-sm text-slate-700">
                    Facture confirmée
                  </label>
                </div>

                <div className="flex items-center justify-end gap-2 pt-2">
                  <button
                    type="button"
                    onClick={() => navigate(-1)}
                    className="rounded-xl border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
                  >
                    Annuler
                  </button>
                  <button
                    type="submit"
                    disabled={saving}
                    className="rounded-xl bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-60"
                  >
                    {saving ? "Enregistrement…" : "Enregistrer"}
                  </button>
                </div>
              </div>

              {/* Colonne droite : achats */}
              <div className="md:col-span-2">
                {loadingPurchases && <LoadingIndicator />}
                {!loadingPurchases && (
                  <div className="rounded-2xl border border-slate-200 bg-white p-4 md:p-6 shadow-sm">
                    <div className="flex items-center justify-between">
                      <h3 className="text-lg font-semibold">Achats du client</h3>
                      {/* Optionnel : action rapide pour confirmer */}
                      <button
                        type="button"
                        onClick={() => setIsConfirmed(true)}
                        className="btn btn-success btn-sm"
                      >
                        Marquer comme confirmée
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
                              <th>Facture</th>
                            </tr>
                          </thead>
                          <tbody>
                            {purchases.map((p) => {
                              const checked = !!p?.invoiceId && Number(p.invoiceId) === invoiceId;
                              return (
                                <tr key={p?.id}>
                                  <td>
                                    <input
                                      type="checkbox"
                                      className="checkbox"
                                      checked={checked}
                                      onChange={(e) => toggleAttach(p, e.target.checked)}
                                    />
                                  </td>
                                  <td>{p?.id}</td>
                                  <td>{p?.name ?? "-"}</td>
                                  <td>{p?.quantity ?? 0}</td>
                                  <td>{safeCurrency(p?.totalHT)}</td>
                                  <td>{safeCurrency(p?.tvaApplied)}</td>
                                  <td>{safeCurrency(p?.totalTva)}</td>
                                  <td>{p?.invoiceId ?? "—"}</td>
                                </tr>
                              );
                            })}
                          </tbody>
                        </table>
                      </div>
                    ) : (
                      <div className="p-6 text-center text-sm opacity-70">Aucun achat à afficher.</div>
                    )}
                  </div>
                )}
              </div>
            </form>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
