import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function Topbar() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <div className="navbar bg-base-100 shadow">
      <div className="flex-1">
        <Link to="/" className="btn btn-ghost text-xl">
          Okayo Lite
        </Link>
      </div>
      <div className="flex-none gap-2">
        <Link to="/profile" className="btn btn-ghost">
          Mon profil
        </Link>
        <button className="btn btn-primary" onClick={handleLogout}>
          Déconnexion
        </button>
      </div>
    </div>
  );
}
