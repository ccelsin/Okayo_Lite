export default function ErrorAlert({ error }) {
  if (!error) {
    return null;
  }
  const message =
    typeof error === "string"
      ? error
      : error instanceof Error
      ? error.message
      : "Une erreur est survenue";
  return (
    <div role="alert" className="alert alert-error shadow-lg">
      <span>{message}</span>
    </div>
  );
}
