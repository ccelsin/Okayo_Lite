export function formatCurrency(value) {
  if (value === null || value === undefined) {
    return "-";
  }
  const parsed = typeof value === "number" ? value : Number(value);
  if (Number.isNaN(parsed)) {
    return "-";
  }
  return new Intl.NumberFormat("fr-FR", { style: "currency", currency: "EUR" }).format(parsed);
}
