import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import DashboardLayout from "../layouts/DashboardLayout";
import LoadingIndicator from "../components/LoadingIndicator";
import ErrorAlert from "../components/ErrorAlert";

import { getInvoice, updateInvoice, downloadInvoicePdf } from "../api/invoices";
import { listPurchasesByCustomerPending, updatePurchase } from "../api/purchases";

const safeCurrency = (n) =>
  new Intl.NumberFormat("fr-FR", { style: "currency", currency: "EUR" }).format(Number(n || 0));

const safePercent = (n) =>
  new Intl.NumberFormat("fr-FR", {
    style: "percent",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(n || 0) / 100);

  
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
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <h1 className="text-2xl md:text-3xl font-bold text-slate-900">
            Modifier la facture
          </h1>
          <div className="flex flex-wrap gap-2">
            <button
              onClick={() => navigate(-1)}
              className="btn btn-outline btn-sm sm:btn-md"
              type="button"
            >
              Retour
            </button>
            <button
              onClick={onDownload}
              className="btn btn-primary btn-sm sm:btn-md"
              type="button"
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
          <div className="mt-4">
            <ErrorAlert error={error} />
          </div>
        ) : (
          <form
            onSubmit={onSave}
            className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-3"
          >
            {/* Colonne gauche : infos facture */}
            <div className="rounded-2xl border border-slate-200 bg-white p-4 md:p-6 shadow-sm space-y-4">
              <div className="form-control">
                <label className="label">
                  <span className="label-text">Référence</span>
                </label>
                <input
                  type="text"
                  value={reference}
                  disabled
                  className="input input-bordered w-full bg-slate-50"
                />
                <label className="label">
                  <span className="label-text-alt text-slate-500">
                    La référence est générée côté serveur et n’est pas modifiable.
                  </span>
                </label>
              </div>

              <div className="form-control">
                <label className="label">
                  <span className="label-text">Client ID</span>
                </label>
                <input
                  type="number"
                  value={customerId ?? ""}
                  disabled
                  className="input input-bordered w-full bg-slate-50"
                />
              </div>

              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <div className="form-control">
                  <label className="label">
                    <span className="label-text">Facturation</span>
                  </label>
                  <input
                    type="date"
                    value={billingDate}
                    onChange={(e) => setBillingDate(e.target.value)}
                    className="input input-bordered w-full"
                  />
                </div>
                <div className="form-control">
                  <label className="label">
                    <span className="label-text">Echéance</span>
                  </label>
                  <input
                    type="date"
                    value={dueDate}
                    onChange={(e) => setDueDate(e.target.value)}
                    className="input input-bordered w-full"
                  />
                </div>
              </div>

              <div className="form-control">
                <label className="label cursor-pointer justify-start gap-3">
                  <input
                    id="isConfirmed"
                    type="checkbox"
                    checked={isConfirmed}
                    onChange={(e) => setIsConfirmed(e.target.checked)}
                    className="checkbox checkbox-sm"
                  />
                  <span className="label-text">Facture confirmée</span>
                </label>
              </div>

              <div className="flex items-center justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => navigate(-1)}
                  className="btn btn-outline"
                >
                  Annuler
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="btn btn-primary disabled:opacity-60"
                >
                  {saving ? "Enregistrement…" : "Enregistrer"}
                </button>
              </div>
            </div>

            {/* Colonne droite : achats */}
            <div className="lg:col-span-2">
              {loadingPurchases && <LoadingIndicator />}
              {!loadingPurchases && (
                <div className="rounded-2xl border border-slate-200 bg-white p-4 md:p-6 shadow-sm">
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                    <h3 className="text-lg font-semibold">Achats du client</h3>
                    <button
                      type="button"
                      onClick={() => setIsConfirmed(true)}
                      className="btn btn-success btn-sm"
                    >
                      Marquer comme confirmée
                    </button>
                  </div>

                  {Array.isArray(purchases) && purchases.length > 0 ? (
                    <div className="mt-3 overflow-x-auto">
                      <table className="table table-zebra w-full">
                        <thead className="text-sm">
                          <tr>
                            <th className="whitespace-nowrap">Affecter</th>
                            <th className="whitespace-nowrap">ID</th>
                            <th>Produit</th>
                            <th className="whitespace-nowrap">Qté</th>
                            <th className="whitespace-nowrap">Total HT</th>
                            <th className="whitespace-nowrap">TVA</th>
                            <th className="whitespace-nowrap">Total TVA</th>
                            <th className="whitespace-nowrap">Facture</th>
                          </tr>
                        </thead>
                        <tbody className="[&>tr>td]:align-middle text-sm">
                          {purchases.map((p) => {
                            const checked =
                              !!p?.invoiceId && Number(p.invoiceId) === invoiceId;
                            return (
                              <tr key={p?.id}>
                                <td>
                                  <input
                                    type="checkbox"
                                    className="checkbox checkbox-sm"
                                    checked={checked}
                                    onChange={(e) => toggleAttach(p, e.target.checked)}
                                  />
                                </td>
                                <td className="whitespace-nowrap">{p?.id}</td>
                                <td className="min-w-[12rem]">{p?.name ?? "-"}</td>
                                <td className="whitespace-nowrap">{p?.quantity ?? 0}</td>
                                <td className="whitespace-nowrap">{safeCurrency(p?.totalHT)}</td>
                                <td className="whitespace-nowrap">{safePercent(p?.tvaApplied)}</td>
                                <td className="whitespace-nowrap">{safeCurrency(p?.totalTva)}</td>
                                <td className="whitespace-nowrap">{p?.invoiceId ?? "—"}</td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <div className="p-6 text-center text-sm opacity-70">
                      Aucun achat à afficher.
                    </div>
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
