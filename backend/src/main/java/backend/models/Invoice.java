package backend.models;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;

/**
 * Représente une facture émise ou reçue dans le système.
 * 
 * <p>Cette entité regroupe les informations relatives à une facture,
 * telles que la référence, les dates, les montants (HT et TTC), 
 * les achats associés, le client, le créateur et les détails de paiement.</p>
 *
 * <p>L’annotation {@link Data} de Lombok permet de générer automatiquement
 * les accesseurs, mutateurs, et méthodes utilitaires (equals, hashCode, toString).</p>
 */
@Entity
@Table(name = "invoices")
@Data
public class Invoice {

    /** Identifiant unique de la facture (clé primaire). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Référence unique de la facture.
     * <p>Non modifiable après création, utilisée comme identifiant métier.</p>
     */
    @Column(nullable = false, unique = true, updatable = false)
    private String reference;

    /** Date d’émission de la facture (facultative). */
    @Nullable
    private Date billingDate;

    /** Date d’échéance du paiement (facultative). */
    @Nullable
    private Date dueDate;

    /**
     * Montant total hors taxe (HT) de la facture.
     * <p>Souvent calculé à partir de la somme des {@link Purchase} associés.</p>
     */
    private BigDecimal totalHT;

    /**
     * Montant total toutes taxes comprises (TTC) de la facture.
     * <p>Correspond généralement à {@code totalHT + totalTVA}.</p>
     */
    private BigDecimal totalTTC;

    /**
     * Client destinataire de la facture.
     * <p>Relation : plusieurs factures peuvent être associées au même {@link User} client.</p>
     */
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = true)
    private User customer;

    /**
     * Utilisateur ayant créé la facture.
     * <p>Relation : un utilisateur (ex. : administrateur ou commerçant) peut créer plusieurs factures.</p>
     * <p>Ce champ est obligatoire.</p>
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User creator;

    /**
     * Liste des achats liés à cette facture.
     * <p>Relation : une facture peut contenir plusieurs {@link Purchase}.</p>
     * <p>L’annotation {@link JsonIgnore} empêche la sérialisation pour éviter les boucles JSON.</p>
     */
    @JsonIgnore
    @OneToMany(mappedBy = "invoice")
    private List<Purchase> purchases;

    /**
     * Détails de paiement associés à la facture.
     * <p>Relation : plusieurs factures peuvent partager les mêmes {@link PaymentDetails}.</p>
     * <p>Ce champ est obligatoire.</p>
     */
    @ManyToOne
    @JoinColumn(name = "payment_details_id", nullable = false)
    private PaymentDetails paymentDetails;

    /**
     * Indique si la facture a été confirmée/validée.
     * <p>Valeur par défaut : {@code false}.</p>
     */
    @Column(nullable = false)
    private boolean isConfirmed = false;
}
