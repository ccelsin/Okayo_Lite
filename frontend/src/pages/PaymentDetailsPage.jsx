import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import DashboardLayout from "../layouts/DashboardLayout";
import LoadingIndicator from "../components/LoadingIndicator";
import ErrorAlert from "../components/ErrorAlert";
import { useAsync } from "../hooks/useAsync";
import { createPaymentDetails, listPaymentDetails } from "../api/paymentDetails";
import * as api from "../api/paymentDetails"; // détecte updatePaymentDetails
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.css";

export default function PaymentDetailsPage() {
  const { data, isLoading, error, refresh } = useAsync(listPaymentDetails, []);
  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { isSubmitting, errors },
  } = useForm({
    defaultValues: {
      paymentName: "",
      paymentTerm: "TOTAL_HT",
      domiciliation: "",
      holderName: "",
      iban: "",
      bic: "",
    },
  });

  const [mode, setMode] = useState("list"); // list | create | edit
  const [selectedId, setSelectedId] = useState(null);
  const canEdit = typeof api.updatePaymentDetails === "function";

  const nf = useMemo(() => new Intl.NumberFormat("fr-FR"), []);

  function fillFormFrom(detail) {
    if (!detail) return;
    setValue("paymentName", detail.paymentName ?? "");
    setValue("paymentTerm", detail.paymentTerm ?? "TOTAL_HT");
    setValue("domiciliation", detail.domiciliation ?? "");
    setValue("holderName", detail.holderName ?? "");
    setValue("iban", detail.iban ?? "");
    setValue("bic", detail.bic ?? "");
  }

  function handleApiError(e, fallbackTitle = "Erreur", fallbackText = "Une erreur est survenue") {
    const status = e?.response?.status ?? e?.status;
    const apiMsg = e?.response?.data?.message || e?.message;
    if (status === 401 || status === 403) {
      Swal.fire({ icon: "error", title: "Accès refusé", text: "Vous n'avez pas les droits pour cette action." });
      return;
    }
    Swal.fire({ icon: "error", title: fallbackTitle, text: apiMsg || fallbackText });
  }

  async function onSubmitCreate(values) {
    try {
      await createPaymentDetails(values);
      await refresh();
      reset({ paymentTerm: "TOTAL_HT" });
      setMode("list");
      Swal.fire({ icon: "success", title: "Détails créés", timer: 1700, showConfirmButton: false });
    } catch (e) {
      handleApiError(e, "Échec de la création", "Impossible de créer les détails de paiement");
    }
  }

  async function onSubmitEdit(values) {
    if (!selectedId) return;
    if (!canEdit) {
      Swal.fire({
        icon: "info",
        title: "Modification indisponible",
        text: "L'API updatePaymentDetails n'est pas exportée par ../api/paymentDetails.",
      });
      return;
    }
    try {
      await api.updatePaymentDetails({ id: selectedId, ...values });
      await refresh();
      setMode("list");
      setSelectedId(null);
      reset({ paymentTerm: "TOTAL_HT" });
      Swal.fire({ icon: "success", title: "Détails modifiés", timer: 1500, showConfirmButton: false });
    } catch (e) {
      handleApiError(e, "Échec de la modification", "Impossible de modifier ces détails");
    }
  }

  const selected = Array.isArray(data) ? data.find((d) => d.id === selectedId) : null;

  async function copy(text, label = "Copié") {
    try {
      await navigator.clipboard.writeText(text || "");
      Swal.fire({ icon: "success", title: label, timer: 900, showConfirmButton: false });
    } catch {
      Swal.fire({ icon: "error", title: "Impossible de copier", timer: 1200, showConfirmButton: false });
    }
  }

  return (
    <DashboardLayout>
      {/* Header responsive */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl sm:text-3xl font-bold">Moyens de paiement</h1>
        <button className="btn btn-outline w-full sm:w-auto" onClick={refresh} disabled={isLoading}>
          {isLoading ? "Chargement..." : "Rafraîchir"}
        </button>
      </div>

      {/* Barre d'actions mobile-first */}
      <div className="mt-4 grid grid-cols-1 gap-2 sm:auto-cols-max sm:inline-grid sm:grid-flow-col">
        <button
          className={`btn w-full sm:w-auto ${mode === "list" ? "btn-primary" : "btn-outline"}`}
          onClick={() => setMode("list")}
        >
          Voir la liste
        </button>
        <button
          className={`btn w-full sm:w-auto ${mode === "create" ? "btn-primary" : "btn-outline"}`}
          onClick={() => {
            reset({ paymentTerm: "TOTAL_HT" });
            setSelectedId(null);
            setMode("create");
          }}
        >
          Ajouter
        </button>
        <button
          className={`btn w-full sm:w-auto ${mode === "edit" ? "btn-primary" : "btn-outline"}`}
          onClick={() => {
            if (!selectedId) {
              Swal.fire({ icon: "info", title: "Aucun élément sélectionné", text: "Sélectionnez un mode de paiement." });
              return;
            }
            fillFormFrom(selected);
            setMode("edit");
          }}
        >
          Modifier
        </button>
      </div>

      <div className="mt-6 grid gap-6 md:grid-cols-3">
        {/* Formulaire (mobile: pleine largeur, desktop: 1/3) */}
        {(mode === "create" || mode === "edit") && (
          <div className="card bg-base-100 shadow md:col-span-1">
            <div className="card-body">
              <h2 className="card-title text-xl">{mode === "create" ? "Nouveaux détails" : `Modifier #${selectedId}`}</h2>
              <form className="space-y-3" onSubmit={handleSubmit(mode === "create" ? onSubmitCreate : onSubmitEdit)}>
                <label className="form-control">
                  <span className="label-text">Nom du paiement</span>
                  <input
                    className={`input input-bordered ${errors.paymentName ? "input-error" : ""}`}
                    placeholder="ex: Virement bancaire"
                    disabled={isSubmitting}
                    {...register("paymentName", { required: "Champ requis" })}
                  />
                </label>

                <label className="form-control">
                  <span className="label-text">Terme</span>
                  <select
                    className="select select-bordered"
                    disabled={isSubmitting}
                    {...register("paymentTerm", { required: "Champ requis" })}
                  >
                    <option value="TOTAL_HT">Total HT</option>
                    <option value="TOTAL_TTC">Total TTC</option>
                  </select>
                </label>

                <label className="form-control">
                  <span className="label-text">Domiciliation</span>
                  <input
                    className="input input-bordered"
                    placeholder="ex: Banque X - Agence Y"
                    disabled={isSubmitting}
                    {...register("domiciliation", { required: "Champ requis" })}
                  />
                </label>

                <label className="form-control">
                  <span className="label-text">Titulaire</span>
                  <input
                    className="input input-bordered"
                    placeholder="Nom du titulaire"
                    disabled={isSubmitting}
                    {...register("holderName", { required: "Champ requis" })}
                  />
                </label>

                <label className="form-control">
                  <span className="label-text">IBAN</span>
                  <input
                    className={`input input-bordered ${errors.iban ? "input-error" : ""}`}
                    placeholder="FR76 3000 6000 ..."
                    disabled={isSubmitting}
                    {...register("iban", {
                      required: "Champ requis",
                      validate: (v) =>
                        !v || /^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$/i.test(v.replace(/\s+/g, "")) || "IBAN invalide",
                    })}
                  />
                </label>

                <label className="form-control">
                  <span className="label-text">BIC</span>
                  <input
                    className={`input input-bordered ${errors.bic ? "input-error" : ""}`}
                    placeholder="ex: BREDFRPP"
                    disabled={isSubmitting}
                    {...register("bic")} // plus de validation
                  />
                </label>

                <div className="flex flex-col sm:flex-row gap-2">
                  <button type="submit" className="btn btn-primary w-full sm:flex-1" disabled={isSubmitting}>
                    {isSubmitting ? (mode === "create" ? "Création..." : "Sauvegarde...") : (mode === "create" ? "Créer" : "Enregistrer")}
                  </button>
                  <button
                    type="button"
                    className="btn btn-ghost w-full sm:w-auto"
                    onClick={() => {
                      reset({ paymentTerm: "TOTAL_HT" });
                      setMode("list");
                      setSelectedId(null);
                    }}
                  >
                    Annuler
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Liste : cartes (mobile) + tableau (desktop amélioré) */}
        <div className={`${mode === "list" ? "block" : "md:col-span-2"} md:col-span-2`}>
          {isLoading && <LoadingIndicator />}
          <ErrorAlert error={error} />

          {Array.isArray(data) && data.length > 0 ? (
            <>
              {/* Cartes (mobile) */}
              <div className="md:hidden space-y-3">
                {data.map((d) => (
                  <div
                    key={d.id}
                    className={`card bg-base-100 shadow ${selectedId === d.id ? "ring ring-primary/40" : ""}`}
                  >
                    <div className="card-body p-4">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <div className="text-xs opacity-60">ID</div>
                          <div className="font-medium">{d.id}</div>
                        </div>
                        <input
                          type="radio"
                          className="radio"
                          name="sel-m"
                          checked={selectedId === d.id}
                          onChange={() => setSelectedId(d.id)}
                        />
                      </div>

                      <div className="mt-3 space-y-1 text-sm">
                        <div>
                          <span className="opacity-60">Nom :</span>{" "}
                          <span className="font-medium">{d.paymentName}</span>
                        </div>
                        <div>
                          <span className="opacity-60">Terme :</span>{" "}
                          <span className="font-medium">{d.paymentTerm}</span>
                        </div>
                        <div>
                          <span className="opacity-60">Domiciliation :</span>{" "}
                          <span className="font-medium">{d.domiciliation}</span>
                        </div>
                        <div>
                          <span className="opacity-60">Titulaire :</span>{" "}
                          <span className="font-medium">{d.holderName}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="opacity-60">IBAN :</span>
                          <span className="font-medium break-all">{d.iban}</span>
                          <button className="btn btn-ghost btn-xs" onClick={() => copy(d.iban, "IBAN copié")}>
                            Copier
                          </button>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="opacity-60">BIC :</span>
                          <span className="font-medium">{d.bic}</span>
                          <button className="btn btn-ghost btn-xs" onClick={() => copy(d.bic, "BIC copié")}>
                            Copier
                          </button>
                        </div>
                      </div>

                      <div className="card-actions justify-end gap-2 pt-2">
                        <button
                          className="btn btn-sm"
                          onClick={() => {
                            setSelectedId(d.id);
                            fillFormFrom(d);
                            if (canEdit) {
                              setMode("edit");
                            } else {
                              Swal.fire({
                                icon: "info",
                                title: "Modification indisponible",
                                text: "updatePaymentDetails n'est pas exposée.",
                              });
                            }
                          }}
                        >
                          Modifier
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {/* Desktop amélioré */}
              <div className="hidden md:block">
                <div className="card bg-base-100 shadow">
                  <div className="card-body pb-0">
                    <div className="flex items-center justify-between gap-3">
                      <h3 className="card-title">Liste des moyens de paiement</h3>
                      <div className="flex items-center gap-2">
                        <span className="text-sm opacity-70">
                          {selectedId ? `Sélection : #${selectedId}` : "Aucune sélection"}
                        </span>
                        <button
                          className="btn btn-outline btn-sm"
                          disabled={!selectedId}
                          onClick={() => {
                            fillFormFrom(selected);
                            if (canEdit) {
                              setMode("edit");
                            } else {
                              Swal.fire({
                                icon: "info",
                                title: "Modification indisponible",
                                text: "updatePaymentDetails n'est pas exposée.",
                              });
                            }
                          }}
                        >
                          Modifier la sélection
                        </button>
                      </div>
                    </div>
                  </div>

                  <div className="overflow-x-auto">
                    <table className="table table-zebra">
                      <thead className="sticky top-0 bg-base-100 z-10">
                        <tr>
                          <th className="w-12">#</th>
                          <th className="min-w-[160px]">Nom</th>
                          <th className="w-36">Terme</th>
                          <th className="min-w-[200px]">Domiciliation</th>
                          <th className="min-w-[160px]">Titulaire</th>
                          <th className="min-w-[220px]">IBAN</th>
                          <th className="min-w-[140px]">BIC</th>
                          <th className="w-28 text-right">Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {data.map((d) => (
                          <tr
                            key={d.id}
                            className={`hover ${selectedId === d.id ? "bg-base-200" : ""}`}
                            onClick={() => setSelectedId(d.id)}
                          >
                            <td>
                              <input
                                type="radio"
                                className="radio"
                                name="sel-d"
                                checked={selectedId === d.id}
                                onChange={() => setSelectedId(d.id)}
                              />
                            </td>
                            <td className="truncate" title={d.paymentName || ""}>
                              {d.paymentName}
                            </td>
                            <td className="whitespace-nowrap">{d.paymentTerm}</td>
                            <td className="truncate" title={d.domiciliation || ""}>
                              {d.domiciliation}
                            </td>
                            <td className="truncate" title={d.holderName || ""}>
                              {d.holderName}
                            </td>
                            <td className="truncate font-mono" title={d.iban || ""}>
                              <div className="flex items-center gap-2">
                                <span className="max-w-[160px] truncate">{d.iban}</span>
                                <button
                                  className="btn btn-ghost btn-xs"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    copy(d.iban, "IBAN copié");
                                  }}
                                >
                                  Copier
                                </button>
                              </div>
                            </td>
                            <td className="truncate font-mono" title={d.bic || ""}>
                              <div className="flex items-center gap-2">
                                <span className="max-w-[120px] truncate">{d.bic}</span>
                                <button
                                  className="btn btn-ghost btn-xs"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    copy(d.bic, "BIC copié");
                                  }}
                                >
                                  Copier
                                </button>
                              </div>
                            </td>
                            <td className="text-right">
                              <button
                                className="btn btn-xs"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setSelectedId(d.id);
                                  fillFormFrom(d);
                                  if (canEdit) {
                                    setMode("edit");
                                  } else {
                                    Swal.fire({
                                      icon: "info",
                                      title: "Modification indisponible",
                                      text: "updatePaymentDetails n'est pas exposée.",
                                    });
                                  }
                                }}
                              >
                                Modifier
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
            !isLoading && (
              <div className="p-6 text-center text-sm text-base-content/70">
                Aucun moyen de paiement enregistré.
              </div>
            )
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
