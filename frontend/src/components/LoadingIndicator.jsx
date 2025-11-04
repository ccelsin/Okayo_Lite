// Loading component when api calls take time

export default function LoadingIndicator({ label = "Chargement..." }) {
  return (
    <div className="flex items-center justify-center py-8">
      <span className="loading loading-spinner loading-lg text-primary" />
      <span className="ml-3 text-lg font-medium">{label}</span>
    </div>
  );
}
