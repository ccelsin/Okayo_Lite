// Error management

import { useEffect } from "react";
import Swal from "sweetalert2";

export default function ErrorAlert({ error }) {
  useEffect(() => {
    if (!error) return;

    const message =
      typeof error === "string"
        ? error
        : error instanceof Error
        ? error.message
        : "Une erreur est survenue";

    // SweetAlert2 alert
    Swal.fire({
      icon: "error",
      title: "Oups…",
      text: message,
      confirmButtonColor: "#d33",
      confirmButtonText: "Fermer",
    });
  }, [error]);

  return null;
}
