import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { register as registerUser } from "../api/auth";
import ErrorAlert from "../components/ErrorAlert";
import { useAuth } from "../context/AuthContext";
import DashboardLayout from "../layouts/DashboardLayout";


export default function RegisterPage() {
  const { register, handleSubmit, formState } = useForm({
    defaultValues: {
      role: "CUSTOMER"
    }
  });
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const { errors, isSubmitting } = formState;
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  useEffect(() => {
    if (isAuthenticated) {
      navigate("/");
    }
  }, [isAuthenticated, navigate]);

  async function onSubmit(values) {
    try {
      setError(null);
      setSuccess(null);
      await registerUser(values);
      setSuccess("Compte créé avec succès. Vous pouvez maintenant vous connecter.");
      setTimeout(() => navigate("/login"), 1500);
    } catch (err) {
      setError(err.response.data.message);
    }
  }

  return (
    <DashboardLayout variant="auth">
    <div className="flex min-h-screen items-center justify-center bg-base-200">
      <div className="card w-full max-w-md bg-base-100 shadow-xl">
        <div className="card-body">
          <h1 className="card-title justify-center text-2xl">Inscription</h1>
          <ErrorAlert error={error} />
          {success && <div className="alert alert-success">{success}</div>}
          <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
            <div className="form-control">
              <label className="label" htmlFor="username">
                <span className="label-text">Nom d'utilisateur</span>
              </label>
              <input
                id="username"
                type="text"
                className="input input-bordered"
                {...register("username", { required: "Champ requis" })}
              />
              {errors.username && <span className="text-sm text-error">{errors.username.message}</span>}
            </div>
            <div className="form-control">
              <label className="label" htmlFor="password">
                <span className="label-text">Mot de passe</span>
              </label>
              <input
                id="password"
                type="password"
                className="input input-bordered"
                {...register("password", { required: "Champ requis", minLength: { value: 6, message: "Au moins 6 caractères" } })}
              />
              {errors.password && <span className="text-sm text-error">{errors.password.message}</span>}
            </div>
            <div className="form-control">
              <label className="label" htmlFor="role">
                <span className="label-text">Rôle</span>
              </label>
              <select id="role" className="select select-bordered" {...register("role")}>
                <option value="ADMIN">Administrateur</option>
                <option value="CUSTOMER">Client</option>
              </select>
            </div>
            <button type="submit" className="btn btn-primary w-full" disabled={isSubmitting}>
              {isSubmitting ? "Création..." : "Créer un compte"}
            </button>
          </form>
          <p className="text-center text-sm text-base-content/70">
            Déjà inscrit ? <Link to="/login" className="link link-primary">Connectez-vous</Link>
          </p>
        </div>
      </div>
    </div>
    </DashboardLayout>
  );
}
