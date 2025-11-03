import { useForm } from "react-hook-form";
import DashboardLayout from "../../layouts/DashboardLayout";
import LoadingIndicator from "../../components/LoadingIndicator";
import ErrorAlert from "../../components/ErrorAlert";
import { useAsync } from "../../hooks/useAsync";
import { createProduct, listProducts } from "../../api/products";
import { formatCurrency } from "../../utils/format";

export default function ProductsPage() {
  const {
    data,
    isLoading,
    error,
    refresh
  } = useAsync(listProducts, []);
  const {
    register,
    handleSubmit,
    reset,
    formState: { isSubmitting }
  } = useForm();

  async function onSubmit(values) {
    await createProduct(values);
    reset();
    await refresh();
  }

  return (
    <DashboardLayout>
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">Produits</h1>
      </div>
      <div className="mt-6 grid gap-6 md:grid-cols-3">
        <div className="card bg-base-100 shadow md:col-span-1">
          <div className="card-body">
            <h2 className="card-title">Nouveau produit</h2>
            <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
              <div className="form-control">
                <label className="label" htmlFor="name">
                  <span className="label-text">Nom</span>
                </label>
                <input id="name" className="input input-bordered" {...register("name", { required: true })} />
              </div>
              <div className="form-control">
                <label className="label" htmlFor="unitPriceHT">
                  <span className="label-text">Prix HT</span>
                </label>
                <input
                  id="unitPriceHT"
                  type="number"
                  step="0.01"
                  className="input input-bordered"
                  {...register("unitPriceHT", { valueAsNumber: true, required: true })}
                />
              </div>
              <div className="form-control">
                <label className="label" htmlFor="tvaId">
                  <span className="label-text">Identifiant TVA</span>
                </label>
                <input
                  id="tvaId"
                  type="number"
                  className="input input-bordered"
                  {...register("tvaId", { valueAsNumber: true, required: true })}
                />
              </div>
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
                    <th>Nom</th>
                    <th>Prix HT</th>
                    <th>TVA</th>
                  </tr>
                </thead>
                <tbody>
                  {data.map((product) => (
                    <tr key={product.id}>
                      <td>{product.id}</td>
                      <td>{product.name}</td>
                      <td>{formatCurrency(product.unitPriceHT)}</td>
                      <td>{product.tvaId}</td>
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
