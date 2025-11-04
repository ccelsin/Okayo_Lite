import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "../layouts/DashboardLayout";
import { isAdmin as checkIsAdmin } from "../api/auth"; // ton endpoint /api/auth/isAdmin

const ADMIN_SHORTCUTS = [
  {
    title: "Produits",
    description: "Gérez le catalogue produit et associez la TVA adaptée.",
    to: "/products"
  },
  {
    title: "Achats",
    description: "Validez les achats réalisés par vos clients.",
    to: "/purchases"
  },
  {
    title: "Moyens de paiement",
    description: "Configurez les coordonnées bancaires de votre entreprise.",
    to: "/payment-details"
  },
  {
    title: "TVA",
    description: "Gérez les taux de TVA en fonction de la réglementation.",
    to: "/tva"
  },
  {
    title: "Factures",
    description: "Gérer les différentes factures des clients.",
    to: "/invoices"
  }
];

const CUSTOMER_SHORTCUTS = [
  {
    title: "Mes achats",
    description: "Consultez l'historique de vos commandes.",
    to: "/my-purchases"
  },
  {
    title: "Articles",
    description: "Consultez les Offres Okayo et commandez",
    to: "/shop"
  },
  {
    title: "Mes factures",
    description: "Consultez vos différentes factures",
    to: "/my-invoices"
  }
];

export default function DashboardPage() {
  const [isAdmin, setIsAdmin] = useState(null); // null = loading, true/false après
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const result = await checkIsAdmin(); // appelle /api/auth/isAdmin
        if (mounted) setIsAdmin(Boolean(result));
      } catch (e) {
        console.error("Erreur lors de la vérification du rôle:", e);
        if (mounted) {
          setError("Impossible de vérifier le rôle utilisateur");
          setIsAdmin(false); // fallback : afficher interface client
        }
      }
    })();
    return () => { mounted = false; };
  }, []);

  // En attente de la réponse du backend
  if (isAdmin === null) {
    return (
      <DashboardLayout>
        <div className="py-10 text-center text-slate-600">Chargement du tableau de bord…</div>
      </DashboardLayout>
    );
  }

  const shortcuts = isAdmin ? ADMIN_SHORTCUTS : CUSTOMER_SHORTCUTS;

  return (
    <DashboardLayout>
      <h1 className="mb-6 text-3xl font-bold">Bienvenue sur Okayo Lite</h1>
      <p className="mb-8 text-lg text-base-content/70">
        Accédez rapidement aux principales fonctionnalités de la plateforme.
      </p>

      {error && (
        <div className="alert alert-error shadow-lg mb-6">
          <span>{error}</span>
        </div>
      )}

      <div className="grid gap-6 md:grid-cols-2">
        {shortcuts.map((item) => (
          <Link
            key={item.to}
            to={item.to}
            className="card bg-base-100 shadow hover:shadow-lg transition-shadow"
          >
            <div className="card-body">
              <h2 className="card-title">{item.title}</h2>
              <p>{item.description}</p>
              <div className="card-actions justify-end">
                <span className="btn btn-primary">Accéder</span>
              </div>
            </div>
          </Link>
        ))}
      </div>
    </DashboardLayout>
  );
}
