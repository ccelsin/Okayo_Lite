import { Link } from "react-router-dom";
import Topbar from "../components/Topbar";

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Topbar />

      {/* Hero */}
      <header className="relative overflow-hidden bg-gradient-to-b from-white to-slate-50">
        {/* Blobs décoratifs */}
        <div className="pointer-events-none absolute inset-0">
          <div className="absolute -top-24 -left-24 h-72 w-72 rounded-full bg-sky-300/30 blur-3xl" />
          <div className="absolute -bottom-20 -right-16 h-72 w-72 rounded-full bg-blue-400/30 blur-3xl" />
        </div>

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 md:py-24">
          <div className="grid md:grid-cols-2 gap-10 items-center">
            {/* Texte */}
            <div>
              <span className="inline-flex items-center rounded-full bg-sky-50 px-3 py-1 text-xs font-medium text-sky-700 ring-1 ring-sky-200">
                Nouveau • Facturation simplifiée
              </span>
              <h1 className="mt-3 text-4xl md:text-5xl font-extrabold tracking-tight text-slate-900">
                Okayo{" "}
                <span className="text-transparent bg-clip-text bg-gradient-to-r from-sky-600 to-blue-700">
                  Lite
                </span>
              </h1>
              <p className="mt-4 text-lg text-slate-600">
                Créez des factures en un clin d’œil, associez vos achats et
                profitez d’une traçabilité claire de bout en bout. Léger, rapide,
                efficace.
              </p>

              <div className="mt-4 flex flex-wrap items-center gap-3 text-sm text-slate-500">
                <span className="inline-flex items-center gap-2">
                  <Dot /> Export PDF propre
                </span>
                <span className="inline-flex items-center gap-2">
                  <Dot /> Traçabilité des achats
                </span>
                <span className="inline-flex items-center gap-2">
                  <Dot /> TVA configurable
                </span>
              </div>

              <div className="mt-6">
                <Link
                  to="/login"
                  className="inline-flex items-center justify-center rounded-xl bg-gradient-to-r from-sky-600 to-blue-700 px-5 py-3 text-white text-sm font-medium shadow-md hover:from-sky-700 hover:to-blue-800 transition-colors"
                >
                  Se connecter
                </Link>
              </div>
            </div>

            {/* Visuel “dashboard” pimpé */}
            <div className="relative">
              <div className="rounded-3xl bg-white/70 ring-1 ring-slate-200 shadow-xl backdrop-blur-sm">
                {/* Header coloré */}
                <div className="h-14 rounded-t-3xl bg-gradient-to-r from-sky-600 to-blue-700 px-5 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="h-2.5 w-2.5 rounded-full bg-white/70" />
                    <div className="h-2.5 w-2.5 rounded-full bg-white/70" />
                    <div className="h-2.5 w-2.5 rounded-full bg-white/70" />
                  </div>
                  <span className="text-white/90 text-sm font-medium">
                    Aperçu facture
                  </span>
                </div>

                {/* Corps */}
                <div className="p-6">
                  {/* Cards KPIs */}
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <Kpi title="Total HT" value="18 647,00 €" />
                    <Kpi title="TVA 20 %" value="3 729,40 €" />
                    <Kpi title="Total TTC" value="22 376,40 €" highlight />
                  </div>

                  {/* Tableau skeleton stylé */}
                  <div className="mt-5 rounded-xl ring-1 ring-slate-200 overflow-hidden">
                    <div className="bg-slate-50 px-4 py-2 text-xs font-semibold uppercase tracking-wider text-slate-600">
                      Détails
                    </div>
                    <div className="p-4 space-y-3">
                      <SkeletonLine />
                      <SkeletonLine />
                      <SkeletonLine wide />
                    </div>
                  </div>
                </div>
              </div>

              {/* Badges flottants */}
              <div className="absolute -right-3 -top-3">
                <div className="animate-bounce rounded-full bg-sky-600 text-white text-xs px-3 py-1 shadow-md">
                  PDF ✔
                </div>
              </div>
              <div className="absolute -left-3 bottom-10">
                <div className="animate-pulse rounded-full bg-blue-700 text-white text-xs px-3 py-1 shadow-md">
                  Traçabilité
                </div>
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* À propos d’Okayo */}
      <section className="py-12 md:py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-2xl md:text-3xl font-bold text-slate-900">Okayo</h2>
          <p className="mt-3 text-slate-600 max-w-3xl">
            Okayo est une plateforme métier pensée pour simplifier la gestion
            opérationnelle : offres, achats, facturation, et suivi — avec une
            expérience moderne, fiable et rapide.
          </p>
        </div>
      </section>

      {/* Okayo Lite */}
      <section className="py-12 md:py-16 bg-white border-t border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="md:flex md:items-start md:justify-between gap-10">
            <div className="md:w-1/2">
              <h2 className="text-2xl md:text-3xl font-bold text-slate-900">
                Okayo <span className="text-sky-700">Lite</span>
              </h2>
              <p className="mt-3 text-slate-600">
                Conçu pour aller à l’essentiel : créer des factures, suivre la TVA,
                associer des achats, et garantir la traçabilité. Idéal pour démarrer vite et bien.
              </p>
              <ul className="mt-6 grid gap-2 text-slate-700">
                <li>• Création et validation de factures</li>
                <li>• Traçabilité des achats affectés</li>
                <li>• Gestion des moyens de paiement &amp; IBAN</li>
                <li>• TVA configurable (taux actuel / futur / période)</li>
                <li>• Export PDF clair avec HT, TVA par taux, TTC</li>
              </ul>
            </div>

            <div className="md:w-1/2 mt-8 md:mt-0">
              <div className="grid sm:grid-cols-2 gap-4">
                <FeatureCard title="Factures PDF" desc="Export clair et professionnel." />
                <FeatureCard title="Affectation achats" desc="Cochez/décochez les achats d’une facture." />
                <FeatureCard title="Paiements" desc="IBAN, titulaire et domiciliation." />
                <FeatureCard title="TVA" desc="Taux actuel/futur avec période." />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="mt-auto border-t border-slate-200 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 text-sm text-slate-500">
          © {new Date().getFullYear()} OkayoLite — Tous droits réservés.
        </div>
      </footer>
    </div>
  );
}

function Dot() {
  return <span className="inline-block h-1.5 w-1.5 rounded-full bg-sky-500" />;
}

function Kpi({ title, value, highlight = false }) {
  return (
    <div
      className={`rounded-2xl p-4 ring-1 shadow-sm ${
        highlight ? "bg-sky-50 ring-sky-200" : "bg-slate-50 ring-slate-200"
      }`}
    >
      <div className="text-xs font-medium text-slate-500">{title}</div>
      <div
        className={`mt-1 text-lg font-semibold ${
          highlight ? "text-sky-700" : "text-slate-900"
        }`}
      >
        {value}
      </div>
    </div>
  );
}

function SkeletonLine({ wide = false }) {
  return (
    <div
      className={`h-10 rounded-lg bg-slate-100 animate-pulse ${
        wide ? "" : "max-w-[80%]"
      }`}
    />
  );
}

function FeatureCard({ title, desc }) {
  return (
    <div className="rounded-2xl bg-white ring-1 ring-slate-200 p-4 shadow-sm hover:shadow-md hover:ring-sky-200 transition-all">
      <div className="text-base font-semibold text-sky-700">{title}</div>
      <div className="mt-1 text-sm text-slate-600">{desc}</div>
    </div>
  );
}
