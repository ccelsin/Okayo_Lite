import { useForm } from "react-hook-form";
import DashboardLayout from "../../layouts/DashboardLayout";
import LoadingIndicator from "../../components/LoadingIndicator";
import ErrorAlert from "../../components/ErrorAlert";
import { useAsync } from "../../hooks/useAsync";
import { createPaymentDetails, listPaymentDetails } from "../../api/paymentDetails";

export default function PaymentDetailsPage() {
  const { data, isLoading, error, refresh } = useAsync(listPaymentDetails, []);
  const {
    register,
    handleSubmit,
    reset,
    formState: { isSubmitting }
  } = useForm({
    defaultValues: {
      paymentTerm: "TOTAL_HT"
    }
  });

  async function onSubmit(values) {
    await createPaymentDetails(values);
    reset({ paymentTerm: "TOTAL_HT" });
    await refresh();
  }

  return (
    <DashboardLayout>
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold">Moyens de paiement</h1>
        <button className="btn btn-outline" onClick={refresh}>
          Rafraîchir
        </button>
      </div>
      <div className="mt-6 grid gap-6 md:grid-cols-3">
        <div className="card bg-base-100 shadow md:col-span-1">
          <div className="card-body">
            <h2 className="card-title">Nouveaux détails</h2>
            <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
              <input className="input input-bordered" placeholder="Nom du paiement" {...register("paymentName", { required: true })} />
              <select className="select select-bordered" {...register("paymentTerm", { required: true })}>
                <option value="TOTAL_HT">Total HT</option>
                <option value="TOTAL_TTC">Total TTC</option>
              </select>
              <input className="input input-bordered" placeholder="Domiciliation" {...register("domiciliation", { required: true })} />
              <input className="input input-bordered" placeholder="Titulaire" {...register("holderName", { required: true })} />
              <input className="input input-bordered" placeholder="IBAN" {...register("iban", { required: true })} />
              <input className="input input-bordered" placeholder="BIC" {...register("bic", { required: true })} />
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
                    <th>Terme</th>
                    <th>Domiciliation</th>
                    <th>Titulaire</th>
                    <th>IBAN</th>
                    <th>BIC</th>
                  </tr>
                </thead>
                <tbody>
                  {data.map((detail) => (
                    <tr key={detail.id}>
                      <td>{detail.id}</td>
                      <td>{detail.paymentName}</td>
                      <td>{detail.paymentTerm}</td>
                      <td>{detail.domiciliation}</td>
                      <td>{detail.holderName}</td>
                      <td>{detail.iban}</td>
                      <td>{detail.bic}</td>
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
