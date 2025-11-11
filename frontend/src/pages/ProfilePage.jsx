import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router-dom"; // <-- pour rediriger
import Swal from "sweetalert2";                 // <-- SweetAlert2
import DashboardLayout from "../layouts/DashboardLayout";
import LoadingIndicator from "../components/LoadingIndicator";
import ErrorAlert from "../components/ErrorAlert";
import { useAsync } from "../hooks/useAsync";
import { getProfile, updateProfile } from "../api/users";
import { useAuth } from "../context/AuthContext";

export default function ProfilePage() {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const { data, isLoading, error } = useAsync(getProfile, []);
  const {
    register,
    handleSubmit,
    reset,
    formState: { isSubmitting }
  } = useForm();

  useEffect(() => {
    if (data) {
      const { username, email, phoneNumber, address, postalCode, city, website } = data;
      reset({ username, email, phoneNumber, address, postalCode, city, website });
    }
  }, [data, reset]);

  function handleLogout() {
    logout();
    navigate("/");
  }

  async function onSubmit(values) {
    try {
      await updateProfile(values);

      // Succès : on affiche une alerte, puis on redirige vers /login
      await Swal.fire({
        icon: "success",
        title: "Profil mis à jour",
        text: "Votre profil a été enregistré. Veuillez vous reconnecter."
      });

      handleLogout();
    } catch (e) {
      // Récupération d’un message d’erreur exploitable
      const apiMsg =
        e?.response?.data?.message ||
        e?.message ||
        "Une erreur est survenue lors de l’enregistrement de votre profil.";

      await Swal.fire({
        icon: "error",
        title: "Échec de l’enregistrement",
        text: apiMsg
      });
    }
  }

  return (
    <DashboardLayout>
      <h1 className="mb-6 text-3xl font-bold">Mon profil</h1>
      {isLoading && <LoadingIndicator />}
      <ErrorAlert error={error} />

      {data && (
        <form className="grid gap-4 md:grid-cols-2" onSubmit={handleSubmit(onSubmit)}>
          <div className="form-control md:col-span-2">
            <label className="label" htmlFor="username">
              <span className="label-text">Nom d'utilisateur</span>
            </label>
            <input
              id="username"
              className="input input-bordered"
              {...register("username", { required: true })}
            />
            <label className="label" htmlFor="username">
              <span className="label-text">
                Après changement de nom vous devez vous déconnecter
              </span>
            </label>
          </div>

          <div className="form-control">
            <label className="label" htmlFor="email">
              <span className="label-text">Email</span>
            </label>
            <input id="email" type="email" className="input input-bordered" {...register("email")} />
          </div>

          <div className="form-control">
            <label className="label" htmlFor="phoneNumber">
              <span className="label-text">Téléphone</span>
            </label>
            <input id="phoneNumber" className="input input-bordered" {...register("phoneNumber")} />
          </div>

          <div className="form-control md:col-span-2">
            <label className="label" htmlFor="address">
              <span className="label-text">Adresse</span>
            </label>
            <input id="address" className="input input-bordered" {...register("address")} />
          </div>

          <div className="form-control">
            <label className="label" htmlFor="postalCode">
              <span className="label-text">Code postal</span>
            </label>
            <input id="postalCode" className="input input-bordered" {...register("postalCode")} />
          </div>

          <div className="form-control">
            <label className="label" htmlFor="city">
              <span className="label-text">Ville</span>
            </label>
            <input id="city" className="input input-bordered" {...register("city")} />
          </div>

          <div className="form-control md:col-span-2">
            <label className="label" htmlFor="website">
              <span className="label-text">Site web</span>
            </label>
            <input id="website" className="input input-bordered" {...register("website")} />
          </div>

          <button type="submit" className="btn btn-primary md:col-span-2" disabled={isSubmitting}>
            {isSubmitting ? "Enregistrement..." : "Enregistrer"}
          </button>
        </form>
      )}
    </DashboardLayout>
  );
}
