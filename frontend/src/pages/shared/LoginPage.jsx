import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import ErrorAlert from "../../components/ErrorAlert";

export default function LoginPage() {
  const { register, handleSubmit, formState } = useForm();
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const { errors, isSubmitting } = formState;
  const [error, setError] = useState(null);

  useEffect(() => {
    if (isAuthenticated) {
      navigate("/");
    }
  }, [isAuthenticated, navigate]);

  async function onSubmit(values) {
    try {
      setError(null);
      await login(values);
      navigate("/");
    } catch (err) {
      setError("Identifiants invalides");
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-base-200">
      <div className="card w-full max-w-md bg-base-100 shadow-xl">
        <div className="card-body">
          <h1 className="card-title justify-center text-2xl">Connexion</h1>
          <ErrorAlert error={error} />
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
                {...register("password", { required: "Champ requis" })}
              />
              {errors.password && <span className="text-sm text-error">{errors.password.message}</span>}
            </div>
            <button type="submit" className="btn btn-primary w-full" disabled={isSubmitting}>
              {isSubmitting ? "Connexion..." : "Se connecter"}
            </button>
          </form>
          <p className="text-center text-sm text-base-content/70">
            Pas de compte ? <Link to="/register" className="link link-primary">Inscrivez-vous</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
