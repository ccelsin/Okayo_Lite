import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useState } from "react";
import logo from "../static/logo-okayo-lite.png";

export default function Topbar() {
  const { logout, user, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  function handleLogout() {
    logout();
    navigate("/");
  }

  const isAdmin = user?.role === "ADMIN";
  const isCustomer = user?.role === "CUSTOMER";

  return (
    <nav className="bg-white border-b border-slate-200 shadow-sm sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 justify-between items-center">
          {/* Logo */}
          <div className="flex items-center gap-3">
            <Link to="/dashboard" className="flex items-center gap-2 py-1 -ml-2">
              <img
                src={logo}
                alt="Okayo Lite"
                className="h-24 sm:h-24 md:h-24 lg:h-36 w-auto object-contain"
              />
            </Link>
          </div>

          {/* Desktop */}
          <div className="hidden md:flex items-center gap-4">
            {isAuthenticated ? (
              <>
                <NavLinks isAdmin={isAdmin} isCustomer={isCustomer} />
                <div className="flex items-center gap-3 ml-4 border-l border-slate-200 pl-4">
                  <Link
                    to="/profile"
                    className="text-slate-600 hover:text-indigo-600 text-sm font-medium"
                  >
                    Profil
                  </Link>
                  <button
                    onClick={handleLogout}
                    className="text-slate-600 hover:text-rose-600 text-sm font-medium"
                  >
                    Déconnexion
                  </button>
                </div>
              </>
            ) : (
              <div className="flex items-center gap-3">
                <Link
                  to="/login"
                  className="text-slate-700 hover:text-indigo-600 text-sm font-medium"
                >
                  Connexion
                </Link>
                <Link
                  to="/register"
                  className="inline-flex items-center justify-center rounded-lg bg-indigo-600 px-3.5 py-2 text-white text-sm font-medium shadow-sm hover:bg-indigo-700"
                >
                  Inscription
                </Link>
              </div>
            )}
          </div>

          {/* Mobile menu button */}
          <button
            className="md:hidden p-2 rounded-md text-slate-600 hover:bg-slate-100"
            onClick={() => setMenuOpen(!menuOpen)}
            aria-label="Menu"
          >
            {menuOpen ? (
              <svg className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            ) : (
              <svg className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            )}
          </button>
        </div>
      </div>

      {/* Mobile dropdown */}
      {menuOpen && (
        <div className="md:hidden border-t border-slate-200 bg-white shadow-sm">
          <div className="px-4 py-3 space-y-2">
            {isAuthenticated ? (
              <>
                <NavLinks
                  isAdmin={isAdmin}
                  isCustomer={isCustomer}
                  onClickLink={() => setMenuOpen(false)}
                />
                <hr className="my-2" />
                <Link
                  to="/profile"
                  onClick={() => setMenuOpen(false)}
                  className="block text-slate-700 hover:text-indigo-600 text-sm font-medium"
                >
                  Profil
                </Link>
                <button
                  onClick={() => {
                    handleLogout();
                    setMenuOpen(false);
                  }}
                  className="block w-full text-left text-slate-700 hover:text-rose-600 text-sm font-medium"
                >
                  Déconnexion
                </button>
              </>
            ) : (
              <>
                <Link
                  to="/login"
                  onClick={() => setMenuOpen(false)}
                  className="block text-slate-700 hover:text-indigo-600 text-sm font-medium"
                >
                  Connexion
                </Link>
                <Link
                  to="/register"
                  onClick={() => setMenuOpen(false)}
                  className="block text-slate-700 hover:text-indigo-600 text-sm font-medium"
                >
                  Inscription
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </nav>
  );
}

function NavLinks({ isAdmin, isCustomer, onClickLink }) {
  return (
    <>
      {isAdmin && (
        <>
          <Link to="/products" onClick={onClickLink} className="text-slate-700 hover:text-indigo-600 text-sm font-medium">Produits</Link>
          <Link to="/purchases" onClick={onClickLink} className="text-slate-700 hover:text-indigo-600 text-sm font-medium">Achats</Link>
          <Link to="/invoices" onClick={onClickLink} className="text-slate-700 hover:text-indigo-600 text-sm font-medium">Factures</Link>
          <Link to="/payment-details" onClick={onClickLink} className="text-slate-700 hover:text-indigo-600 text-sm font-medium">Paiements</Link>
          <Link to="/tva" onClick={onClickLink} className="text-slate-700 hover:text-indigo-600 text-sm font-medium">TVA</Link>
        </>
      )}

      {isCustomer && (
        <>
          <Link to="/my-purchases" onClick={onClickLink} className="text-slate-700 hover:text-indigo-600 text-sm font-medium">Mes achats</Link>
          <Link to="/shop" onClick={onClickLink} className="text-slate-700 hover:text-indigo-600 text-sm font-medium">Articles</Link>
          <Link to="/my-invoices" onClick={onClickLink} className="text-slate-700 hover:text-indigo-600 text-sm font-medium">Mes factures</Link>
        </>
      )}
    </>
  );
}
