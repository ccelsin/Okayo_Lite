import { useForm } from "react-hook-form";
import DashboardLayout from "../../layouts/DashboardLayout";
import LoadingIndicator from "../../components/LoadingIndicator";
import ErrorAlert from "../../components/ErrorAlert";
import { useAsync } from "../../hooks/useAsync";
import { createTva, deleteTva, listTva } from "../../api/tva";

export default function TvaPage() {
  const { data, isLoading, error, refresh } = useAsync(listTva, []);
  const {
    register,
    handleSubmit,
    reset,
    formState: { isSubmitting }
  } = useForm();

  async function onSubmit(values) {
    const payload = {
      ...values,
      startEvolutionDate: values.startEvolutionDate || null,
      endEvolutionDate: values.endEvolutionDate || null
    };
    await createTva(payload);
    reset();
    await refresh();
  }

  async function handleDelete(id) {
    await deleteTva(id);
    await refresh();
  }

  return (
    <DashboardLayout>
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">TVA</h1>
        <button className="btn btn-outline" onClick={refresh}>
          Rafraîchir
        </button>
      </div>
      <div className="mt-6 grid gap-6 md:grid-cols-3">
        <div className="card bg-base-100 shadow md:col-span-1">
          <div className="card-body">
            <h2 className="card-title">Nouveau taux</h2>
            <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
              <input
                type="number"
                step="0.01"
                className="input input-bordered"
                placeholder="Taux précédent"
                {...register("previousRate", { required: true, valueAsNumber: true })}
              />
              <input
                type="number"
                step="0.01"
                className="input input-bordered"
                placeholder="Taux actuel"
                {...register("defaultRate", { required: true, valueAsNumber: true })}
              />
              <input
                type="number"
                step="0.01"
                className="input input-bordered"
                placeholder="Taux futur"
                {...register("futureRate", { required: true, valueAsNumber: true })}
              />
              <input
                type="date"
                className="input input-bordered"
                placeholder="Début évolution"
                {...register("startEvolutionDate")}
              />
              <input
                type="date"
                className="input input-bordered"
                placeholder="Fin évolution"
                {...register("endEvolutionDate")}
              />
              <button type="submit" className="btn btn-primary w-full" disabled={isSubmitting}>
                {isSubmitting ? "Création..." : "Créer"}
              </button>
            </form>
          </div>
        </div>
        <div className="md:col-span-2">
          {isLoading && <LoadingIndicator />}
          <ErrorAlert error={error} />
          {data && (
            <div className="overflow-x-auto">
              <table className="table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Précédent</th>
                    <th>Actuel</th>
                    <th>Futur</th>
                    <th>Début</th>
                    <th>Fin</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {data.map((tva) => (
                    <tr key={tva.id}>
                      <td>{tva.id}</td>
                      <td>{tva.previousRate}</td>
                      <td>{tva.defaultRate}</td>
                      <td>{tva.futureRate}</td>
                      <td>{tva.startEvolutionDate ? new Date(tva.startEvolutionDate).toLocaleDateString() : "-"}</td>
                      <td>{tva.endEvolutionDate ? new Date(tva.endEvolutionDate).toLocaleDateString() : "-"}</td>
                      <td>
                        <button className="btn btn-sm btn-outline btn-error" onClick={() => handleDelete(tva.id)}>
                          Supprimer
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
}
