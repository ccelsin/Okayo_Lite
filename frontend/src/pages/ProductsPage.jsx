import { useMemo, useState } from "react";
import { useForm } from "react-hook-form";
import DashboardLayout from "../layouts/DashboardLayout";
import LoadingIndicator from "../components/LoadingIndicator";
import ErrorAlert from "../components/ErrorAlert";
import { useAsync } from "../hooks/useAsync";
import { createProduct, listProducts } from "../api/products";
import * as api from "../api/products"; // pour détecter updateProduct
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.css";
import { formatCurrency } from "../utils/format";

export default function ProductsPage() {
  const { data, isLoading, error, refresh } = useAsync(listProducts, []);
  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { isSubmitting, errors },
  } = useForm({
    defaultValues: {
      name: "",
      unitPriceHT: "",
      tvaId: "",
    },
  });

  const [mode, setMode] = useState("list"); // list | create | edit
  const [selectedId, setSelectedId] = useState(null);
  const canEdit = typeof api.updateProduct === "function";

  const nf = useMemo(() => new Intl.NumberFormat("fr-FR"), []);

  function fillFormFrom(p) {
    if (!p) return;
    setValue("name", p.name ?? "");
    setValue("unitPriceHT", p.unitPriceHT ?? "");
    setValue("tvaId", p.tvaId ?? "");
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
      // conversions sûres
      const payload = {
        name: values.name,
        unitPriceHT: values.unitPriceHT === "" ? null : Number(values.unitPriceHT),
        tvaId: values.tvaId === "" ? null : Number(values.tvaId),
      };
      await createProduct(payload);
      await refresh();
      reset();
      setMode("list");
      Swal.fire({ icon: "success", title: "Produit créé", timer: 1700, showConfirmButton: false });
    } catch (e) {
      handleApiError(e, "Échec de la création", "Impossible de créer le produit");
    }
  }

  async function onSubmitEdit(values) {
    if (!selectedId) return;
    if (!canEdit) {
      Swal.fire({ icon: "info", title: "Modification indisponible", text: "updateProduct n'est pas exportée par ../api/products." });
      return;
    }
    try {
      const payload = {
        id: selectedId,
        name: values.name,
        unitPriceHT: values.unitPriceHT === "" ? null : Number(values.unitPriceHT),
        tvaId: values.tvaId === "" ? null : Number(values.tvaId),
      };
      await api.updateProduct(payload); // Cas “PUT /products” avec id dans le body
      await refresh();
      setMode("list");
      setSelectedId(null);
      reset();
      Swal.fire({ icon: "success", title: "Produit modifié", timer: 1500, showConfirmButton: false });
    } catch (e) {
      handleApiError(e, "Échec de la modification", "Impossible de modifier le produit");
    }
  }

  const selected = Array.isArray(data) ? data.find((p) => p.id === selectedId) : null;

  return (
    <DashboardLayout>
      {/* Header responsive */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="text-2xl sm:text-3xl font-bold">Produits</h1>
        <button className="btn btn-outline w-full sm:w-auto" onClick={refresh} disabled={isLoading}>
          {isLoading ? "Chargement..." : "Rafraîchir"}
        </button>
      </div>

      {/* Barre d'actions */}
      <div className="mt-4 grid grid-cols-1 gap-2 sm:auto-cols-max sm:inline-grid sm:grid-flow-col">
        <button className={`btn w-full sm:w-auto ${mode === "list" ? "btn-primary" : "btn-outline"}`} onClick={() => setMode("list")}>
          Voir la liste
        </button>
        <button
          className={`btn w-full sm:w-auto ${mode === "create" ? "btn-primary" : "btn-outline"}`}
          onClick={() => {
            reset();
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
              Swal.fire({ icon: "info", title: "Aucun élément sélectionné", text: "Sélectionnez un produit dans la liste." });
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
              <h2 className="card-title text-xl">{mode === "create" ? "Nouveau produit" : `Modifier #${selectedId}`}</h2>

              <form className="space-y-3" onSubmit={handleSubmit(mode === "create" ? onSubmitCreate : onSubmitEdit)}>
                <label className="form-control">
                  <span className="label-text">Nom</span>
                  <input
                    id="name"
                    className={`input input-bordered ${errors.name ? "input-error" : ""}`}
                    placeholder="ex: Clavier mécanique"
                    disabled={isSubmitting}
                    {...register("name", { required: "Champ requis" })}
                  />
                </label>

                <label className="form-control">
                  <span className="label-text">Prix HT</span>
                  <input
                    id="unitPriceHT"
                    type="number"
                    min="0"
                    step="0.01"
                    inputMode="decimal"
                    className={`input input-bordered ${errors.unitPriceHT ? "input-error" : ""}`}
                    placeholder="ex: 49.90"
                    disabled={isSubmitting}
                    {...register("unitPriceHT", {
                      required: "Champ requis",
                      // pas de valueAsNumber pour accepter vide puis convertir en submit
                      validate: (v) => v === "" || Number(v) >= 0 || "Doit être ≥ 0",
                    })}
                  />
                </label>

                <label className="form-control">
                  <span className="label-text">Identifiant TVA</span>
                  <input
                    id="tvaId"
                    type="number"
                    min="1"
                    step="1"
                    inputMode="numeric"
                    className={`input input-bordered ${errors.tvaId ? "input-error" : ""}`}
                    placeholder="ex: 1"
                    disabled={isSubmitting}
                    {...register("tvaId", {
                      required: "Champ requis",
                      validate: (v) => v === "" || Number.isInteger(Number(v)) || "Entier requis",
                    })}
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
                      reset();
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
                {data.map((p) => (
                  <div key={p.id} className={`card bg-base-100 shadow ${selectedId === p.id ? "ring ring-primary/40" : ""}`}>
                    <div className="card-body p-4">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <div className="text-xs opacity-60">ID</div>
                          <div className="font-medium">{p.id}</div>
                        </div>
                        <input
                          type="radio"
                          className="radio"
                          name="sel-m"
                          checked={selectedId === p.id}
                          onChange={() => setSelectedId(p.id)}
                        />
                      </div>

                      <div className="mt-3 space-y-1 text-sm">
                        <div><span className="opacity-60">Nom :</span> <span className="font-medium">{p.name}</span></div>
                        <div><span className="opacity-60">Prix HT :</span> <span className="font-medium">{formatCurrency(p.unitPriceHT)}</span></div>
                        <div><span className="opacity-60">TVA :</span> <span className="font-medium">{p.tvaId}</span></div>
                      </div>

                      <div className="card-actions justify-end gap-2 pt-2">
                        <button
                          className="btn btn-sm"
                          onClick={() => {
                            setSelectedId(p.id);
                            fillFormFrom(p);
                            if (canEdit) {
                              setMode("edit");
                            } else {
                              Swal.fire({ icon: "info", title: "Modification indisponible", text: "updateProduct n'est pas exposée." });
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
                      <h3 className="card-title">Liste des produits</h3>
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
                              Swal.fire({ icon: "info", title: "Modification indisponible", text: "updateProduct n'est pas exposée." });
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
                          <th className="min-w-[200px]">Nom</th>
                          <th className="w-40">Prix HT</th>
                          <th className="w-28">TVA</th>
                          <th className="w-28 text-right">Actions</th>
                        </tr>
                      </thead>
                      <tbody>
                        {data.map((p) => (
                          <tr
                            key={p.id}
                            className={`hover ${selectedId === p.id ? "bg-base-200" : ""}`}
                            onClick={() => setSelectedId(p.id)}
                          >
                            <td>
                              <input
                                type="radio"
                                className="radio"
                                name="sel-d"
                                checked={selectedId === p.id}
                                onChange={() => setSelectedId(p.id)}
                              />
                            </td>
                            <td className="truncate" title={p.name || ""}>{p.name}</td>
                            <td className="whitespace-nowrap">{formatCurrency(p.unitPriceHT)}</td>
                            <td className="whitespace-nowrap">{p.tvaId}</td>
                            <td className="text-right">
                              <button
                                className="btn btn-xs"
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setSelectedId(p.id);
                                  fillFormFrom(p);
                                  if (canEdit) {
                                    setMode("edit");
                                  } else {
                                    Swal.fire({ icon: "info", title: "Modification indisponible", text: "updateProduct n'est pas exposée." });
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
            !isLoading && <div className="p-6 text-center text-sm text-base-content/70">Aucun produit enregistré.</div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
