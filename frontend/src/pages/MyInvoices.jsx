import React, { useEffect, useState } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import { getMyInvoices, downloadInvoicePdf } from "../api/invoices";

export default function MyInvoices() {
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    (async () => {
      try {
        setLoading(true);
        setError("");
        const data = await getMyInvoices(); // response.data
        if (active) setInvoices(Array.isArray(data) ? data : []);
      } catch (e) {
        console.error(e);
        if (active) setError(e?.message || "Une erreur est survenue");
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => { active = false; };
  }, []);

  const onDownload = async (inv) => {
    try {
      const blob = await downloadInvoicePdf(inv.id); // Blob
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${getInvoiceRef(inv)}.pdf`;
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
        <div className="mx-auto max-w-5xl px-4 py-6 md:py-10">
          <h1 className="mb-4 md:mb-6 text-2xl md:text-3xl font-bold text-slate-900">Mes factures</h1>

          {loading && (
            <div className="rounded-2xl border border-slate-200 bg-white p-4 md:p-6 shadow-sm">
              Chargement…
            </div>
          )}

          {!loading && error && (
            <div className="rounded-2xl border border-rose-200 bg-rose-50 p-3 md:p-4 text-rose-700">
              {error}
            </div>
          )}

          {!loading && !error && (
            <>
              {/* --- Mobile cards (default) --- */}
              <ul className="space-y-3 md:hidden">
                {invoices.length === 0 ? (
                  <li className="rounded-2xl border border-slate-200 bg-white p-4 text-center text-slate-500">
                    Aucune facture pour le moment.
                  </li>
                ) : (
                  invoices.map((inv) => (
                    <li key={inv.id} className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <p className="text-sm text-slate-500">Référence</p>
                          <p className="text-base font-semibold text-slate-900">{getInvoiceRef(inv)}</p>
                        </div>
                        <button
                          onClick={() => onDownload(inv)}
                          className="shrink-0 rounded-lg bg-indigo-600 px-3 py-2 text-xs font-medium text-white shadow-sm hover:bg-indigo-700 active:scale-[.99]"
                        >
                          Télécharger
                        </button>
                      </div>

                      <div className="mt-3 grid grid-cols-2 gap-3">
                        <div>
                          <p className="text-xs text-slate-500">Date</p>
                          <p className="text-sm text-slate-800">{formatDate(getInvoiceDate(inv))}</p>
                        </div>
                        <div className="text-right">
                          <p className="text-xs text-slate-500">Montant TTC</p>
                          <p className="text-sm font-semibold text-slate-900">{formatCurrency(getInvoiceAmountTTC(inv))}</p>
                        </div>
                      </div>
                    </li>
                  ))
                )}
              </ul>

              {/* --- Desktop table (md+) --- */}
              <div className="hidden md:block overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm mt-4 md:mt-6">
                <table className="min-w-full divide-y divide-slate-200">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-600">N°</th>
                      <th className="px-6 py-3 text-left text-xs font-semibold uppercase tracking-wider text-slate-600">Date</th>
                      <th className="px-6 py-3 text-right text-xs font-semibold uppercase tracking-wider text-slate-600">Montant TTC</th>
                      <th className="px-6 py-3 text-right text-xs font-semibold uppercase tracking-wider text-slate-600">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {invoices.length === 0 ? (
                      <tr>
                        <td colSpan={4} className="px-6 py-12 text-center text-slate-500">
                          Aucune facture pour le moment.
                        </td>
                      </tr>
                    ) : (
                      invoices.map((inv) => (
                        <tr key={inv.id} className="hover:bg-slate-50/60">
                          <td className="px-6 py-4 text-sm font-medium text-slate-900">
                            {getInvoiceRef(inv)}
                          </td>
                          <td className="px-6 py-4 text-sm text-slate-700">
                            {formatDate(getInvoiceDate(inv))}
                          </td>
                          <td className="px-6 py-4 text-right text-sm font-semibold text-slate-900">
                            {formatCurrency(getInvoiceAmountTTC(inv))}
                          </td>
                          <td className="px-6 py-4 text-right">
                            <button
                              onClick={() => onDownload(inv)}
                              className="rounded-xl bg-indigo-600 px-3 py-2 text-sm font-medium text-white shadow-sm hover:bg-indigo-700 active:scale-[.99]"
                            >
                              Télécharger PDF
                            </button>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}

/* ----- Helpers ----- */

// Référence affichée et utilisée pour le nom de fichier
const getInvoiceRef = (inv) => inv?.reference || inv?.id;

// Date d’affichage : priorité à billingDate
const getInvoiceDate = (inv) => inv?.billingDate || inv?.dueDate || inv?.createdAt;

// Montant TTC (fallback HT si TTC manquant)
const getInvoiceAmountTTC = (inv) => inv?.totalTTC ?? inv?.totalHT ?? 0;

const formatCurrency = (value, currency = "EUR", locale = navigator.language || "fr-FR") =>
  new Intl.NumberFormat(locale, { style: "currency", currency }).format(Number(value || 0));

const formatDate = (iso) => {
  if (!iso) return "—";
  const d = new Date(iso);
  return new Intl.DateTimeFormat(navigator.language || "fr-FR", {
    year: "numeric",
    month: "short",
    day: "2-digit",
  }).format(d);
};
