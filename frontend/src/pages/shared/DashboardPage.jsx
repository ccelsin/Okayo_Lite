import { Link } from "react-router-dom";
import DashboardLayout from "../../layouts/DashboardLayout";

const shortcuts = [
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
    title: "Mes achats",
    description: "Consultez l'historique de vos commandes.",
    to: "/my-purchases"
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
  }
];

export default function DashboardPage() {
  return (
    <DashboardLayout>
      <h1 className="mb-6 text-3xl font-bold">Bienvenue sur Okayo Lite</h1>
      <p className="mb-8 text-lg text-base-content/70">
        Accédez rapidement aux principales fonctionnalités de la plateforme.
      </p>
      <div className="grid gap-6 md:grid-cols-2">
        {shortcuts.map((item) => (
          <Link key={item.to} to={item.to} className="card bg-base-100 shadow hover:shadow-lg transition-shadow">
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
