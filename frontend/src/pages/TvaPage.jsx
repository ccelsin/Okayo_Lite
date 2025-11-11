import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import DashboardLayout from "../layouts/DashboardLayout";
import LoadingIndicator from "../components/LoadingIndicator";
import ErrorAlert from "../components/ErrorAlert";
import { useAsync } from "../hooks/useAsync";
import { createTva, listTva, updateTva } from "../api/tva";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.css";

export default function TvaPage() {
  const { data, isLoading, error, refresh } = useAsync(listTva, []);
  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { isSubmitting, errors },
    watch,
  } = useForm({
    defaultValues: {
      previousRate: "",
      defaultRate: "",
      futureRate: "",
      startEvolutionDate: "",
      endEvolutionDate: "",
    },
  });

  const safePercent = (n) =>
  new Intl.NumberFormat("fr-FR", {
    style: "percent",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(n || 0) / 100);

  const [mode, setMode] = useState("list"); // list | create | edit
  const [selectedId, setSelectedId] = useState(null);

  const nfNumber = useMemo(
    () => new Intl.NumberFormat("fr-FR", { minimumFractionDigits: 2, maximumFractionDigits: 2 }),
    []
  );

  function fillFormFromTva(tva) {
    if (!tva) return;
    setValue("previousRate", tva.previousRate ?? "");
    setValue("defaultRate", tva.defaultRate ?? "");
    setValue("futureRate", tva.futureRate ?? "");
    setValue(
      "startEvolutionDate",
      tva.startEvolutionDate ? new Date(tva.startEvolutionDate).toISOString().slice(0, 10) : ""
    );
    setValue(
      "endEvolutionDate",
      tva.endEvolutionDate ? new Date(tva.endEvolutionDate).toISOString().slice(0, 10) : ""
    );
  }

  async function onSubmitCreate(values) {
    const payload = {
      previousRate: values.previousRate === "" ? null : Number(values.previousRate),
      defaultRate: Number(values.defaultRate),
      futureRate: values.futureRate === "" ? null : Number(values.futureRate),
      startEvolutionDate: values.startEvolutionDate || null,
      endEvolutionDate: values.endEvolutionDate || null,
    };

    try {
      await createTva(payload);
      await refresh();
      reset();
      Swal.fire({ icon: "success", title: "Taux créé", timer: 1800, showConfirmButton: false });
      setMode("list");
    } catch (e) {
      const msg = e?.data.message || "Erreur lors de la création";
      Swal.fire({ icon: "error", title: "Échec", text: msg });
    }
  }

  async function onSubmitEdit(values) {
    if (!selectedId) return;
    const payload = {
      id: selectedId,
      previousRate: values.previousRate === "" ? null : Number(values.previousRate),
      defaultRate: Number(values.defaultRate),
      futureRate: values.futureRate === "" ? null : Number(values.futureRate),
      startEvolutionDate: values.startEvolutionDate || null,
      endEvolutionDate: values.endEvolutionDate || null,
    };

    try {
      await updateTva(payload);
      await refresh();
      Swal.fire({ icon: "success", title: "Taux modifié", timer: 1600, showConfirmButton: false });
      setMode("list");
      setSelectedId(null);
      reset();
    } catch (e) {
      const msg = e?.data.message || "Erreur lors de la modification";
      Swal.fire({ icon: "error", title: "Échec", text: msg });
    }
  }

  const startDate = watch("startEvolutionDate");
  const endDate = watch("endEvolutionDate");

  const selectedTva = Array.isArray(data) ? data.find((t) => t.id === selectedId) : null;

  return (
    <DashboardLayout>
      {/* Header responsive */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl sm:text-3xl font-bold">TVA</h1>
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
          onClick={() => { reset(); setSelectedId(null); setMode("create"); }}
        >
          Ajouter
        </button>
        <button
          className={`btn w-full sm:w-auto ${mode === "edit" ? "btn-primary" : "btn-outline"}`}
          onClick={() => {
            if (!selectedId) {
              Swal.fire({ icon: "info", title: "Aucun élément sélectionné", text: "Sélectionnez un taux dans la liste." });
              return;
            }
            fillFormFromTva(selectedTva);
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
              <h2 className="card-title text-xl">{mode === "create" ? "Nouveau taux" : `Modifier #${selectedId}`}</h2>

              <form className="space-y-3" onSubmit={handleSubmit(mode === "create" ? onSubmitCreate : onSubmitEdit)}>
                <label className="form-control w-full">
                  <span className="label-text">Taux précédent</span>
                  <input
                    type="number"
                    min="0"
                    max="99.99"
                    step="0.01"
                    inputMode="decimal"
                    className={`input input-bordered w-full ${errors.previousRate ? "input-error" : ""}`}
                    placeholder="ex: 19.6"
                    disabled={isSubmitting}
                    {...register("previousRate", {
                      validate: (v) =>
                        v === "" || v === null || (Number(v) >= 0 && Number(v) <= 99.99) || "Doit être entre 0 et 99.99",
                    })}
                  />
                </label>

                <label className="form-control w-full">
                  <span className="label-text">Taux actuel (obligatoire)</span>
                  <input
                    type="number"
                    min="0"
                    max="99.99"
                    step="0.01"
                    inputMode="decimal"
                    className={`input input-bordered w-full ${errors.defaultRate ? "input-error" : ""}`}
                    placeholder="ex: 20.0"
                    disabled={isSubmitting}
                    {...register("defaultRate", {
                      required: "Champ requis",
                      validate: (v) =>
                        v !== "" && Number(v) >= 0 && Number(v) <= 99.99 || "Doit être entre 0 et 99.99",
                    })}
                  />
                </label>

                <label className="form-control w-full">
                  <span className="label-text">Taux futur</span>
                  <input
                    type="number"
                    min="0"
                    max="99.99"
                    step="0.01"
                    inputMode="decimal"
                    className={`input input-bordered w-full ${errors.futureRate ? "input-error" : ""}`}
                    placeholder="ex: 21.0"
                    disabled={isSubmitting}
                    {...register("futureRate", { // ✅ corrige la typo: c'était "previousRate"
                      validate: (v) =>
                        v === "" || v === null || (Number(v) >= 0 && Number(v) <= 99.99) || "Doit être entre 0 et 99.99",
                    })}
                  />
                </label>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <label className="form-control w-full">
                    <span className="label-text">Début évolution</span>
                    <input type="date" className="input input-bordered w-full" disabled={isSubmitting} {...register("startEvolutionDate")} />
                  </label>
                  <label className="form-control w-full">
                    <span className="label-text">Fin évolution</span>
                    <input type="date" className="input input-bordered w-full" disabled={isSubmitting} {...register("endEvolutionDate")} />
                  </label>
                </div>

                <div className="flex flex-col sm:flex-row gap-2">
                  <button type="submit" className="btn btn-primary w-full sm:flex-1" disabled={isSubmitting}>
                    {isSubmitting ? (mode === "create" ? "Création..." : "Sauvegarde...") : (mode === "create" ? "Créer" : "Enregistrer")}
                  </button>
                  <button type="button" className="btn btn-ghost w-full sm:w-auto" onClick={() => { reset(); setMode("list"); setSelectedId(null); }}>
                    Annuler
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Liste: mobile => cartes, desktop => tableau (uniformisé) */}
        <div className={`${mode === "list" ? "block" : "md:col-span-2"} md:col-span-2`}>
          {isLoading && <LoadingIndicator />}
          <ErrorAlert error={error} />

          {Array.isArray(data) && data.length > 0 ? (
            <>
              {/* Cartes (mobile) */}
              <div className="md:hidden space-y-3">
                {data.map((tva) => (
                  <div key={tva.id} className={`card bg-base-100 shadow ${selectedId === tva.id ? "ring ring-primary/40" : ""}`}>
                    <div className="card-body p-4">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <div className="text-xs opacity-60">ID</div>
                          <div className="font-medium">{tva.id}</div>
                        </div>
                        <input
                          type="radio"
                          name="selected-mobile"
                          className="radio"
                          checked={selectedId === tva.id}
                          onChange={() => setSelectedId(tva.id)}
                        />
                      </div>

                      <div className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm mt-3">
                        <div className="opacity-60">Précédent</div>
                        <div className="font-medium">{safePercent(nfNumber.format(tva.previousRate ?? 0))}</div>
                        <div className="opacity-60">Actuel</div>
                        <div className="font-medium">{nfNumber.format(tva.defaultRate ?? 0)}</div>
                        <div className="opacity-60">Futur</div>
                        <div className="font-medium">{nfNumber.format(tva.futureRate ?? 0)}</div>
                        <div className="opacity-60">Début</div>
                        <div className="font-medium">{tva.startEvolutionDate ? new Date(tva.startEvolutionDate).toLocaleDateString("fr-FR") : "-"}</div>
                        <div className="opacity-60">Fin</div>
                        <div className="font-medium">{tva.endEvolutionDate ? new Date(tva.endEvolutionDate).toLocaleDateString("fr-FR") : "-"}</div>
                      </div>

                      <div className="card-actions justify-end gap-2 pt-2">
                        <button className="btn btn-sm" onClick={() => { setSelectedId(tva.id); fillFormFromTva(tva); setMode("edit"); }}>
                          Modifier
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {/* Desktop UNIFORMISÉ */}
              <div className="hidden md:block">
                <div className="card bg-base-100 shadow">
                  <div className="card-body pb-0">
                    <div className="flex items-center justify-between gap-3">
                      <h3 className="card-title">Liste des taux de TVA</h3>
                      <div className="flex items-center gap-2">
                        <span className="text-sm opacity-70">
                          {selectedId ? `Sélection : #${selectedId}` : "Aucune sélection"}
                        </span>
                        <button
                          className="btn btn-outline btn-sm"
                          disabled={!selectedId}
                          onClick={() => { fillFormFromTva(selectedTva); setMode("edit"); }}
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
                          <th className="w-16">ID</th>
                          <th>Précédent</th>
                          <th>Actuel</th>
                          <th>Futur</th>
                          <th>Début</th>
                          <th>Fin</th>
                          <th className="w-28 text-right">Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {data.map((tva) => (
                          <tr
                            key={tva.id}
                            className={`hover ${selectedId === tva.id ? "bg-base-200" : ""}`}
                            onClick={() => setSelectedId(tva.id)}
                          >
                            <td>
                              <input
                                type="radio"
                                name="selected-desktop"
                                className="radio"
                                checked={selectedId === tva.id}
                                onChange={() => setSelectedId(tva.id)}
                              />
                            </td>
                            <td>{tva.id}</td>
                            <td>{nfNumber.format(tva.previousRate ?? 0)}</td>
                            <td>{nfNumber.format(tva.defaultRate ?? 0)}</td>
                            <td>{nfNumber.format(tva.futureRate ?? 0)}</td>
                            <td>{tva.startEvolutionDate ? new Date(tva.startEvolutionDate).toLocaleDateString("fr-FR") : "-"}</td>
                            <td>{tva.endEvolutionDate ? new Date(tva.endEvolutionDate).toLocaleDateString("fr-FR") : "-"}</td>
                            <td className="text-right">
                              <button
                                className="btn btn-xs"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setSelectedId(tva.id);
                                  fillFormFromTva(tva);
                                  setMode("edit");
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
            !isLoading && <div className="p-6 text-center text-sm text-base-content/70">Aucun taux enregistré.</div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
