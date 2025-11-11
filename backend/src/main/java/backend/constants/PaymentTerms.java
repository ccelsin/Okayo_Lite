package backend.constants;

/**
 * Enumération représentant les différentes conditions de paiement disponibles.
 *
 * <p>Cette énumération permet d’indiquer le mode de calcul du montant dû
 * lors d’une transaction ou d’une facture.</p>
 *
 * <ul>
 *   <li>{@link #TOTAL_HT} – Paiement basé sur le montant <strong>Hors Taxes</strong>.</li>
 *   <li>{@link #TOTAL_TTC} – Paiement basé sur le montant <strong>Toutes Taxes Comprises</strong>.</li>
 * </ul>
 */
public enum PaymentTerms {
    /** Paiement effectué sur le total hors taxes (HT). */
    TOTAL_HT,

    /** Paiement effectué sur le total toutes taxes comprises (TTC). */
    TOTAL_TTC
}
