package backend.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import backend.constants.PaymentTerms;

/**
 * Représente les informations de paiement associées à un utilisateur.
 * 
 * <p>Cette entité contient les détails bancaires et les conditions de paiement
 * qui peuvent être utilisés lors de la création de factures. Elle relie un utilisateur
 * à un ou plusieurs modes de paiement et à leurs factures associées.</p>
 *
 * <p>L’annotation {@link Data} de Lombok permet de générer automatiquement
 * les accesseurs, mutateurs et méthodes utilitaires (equals, hashCode, toString).</p>
 */
@Entity
@Table(name = "payment_details")
@Data
public class PaymentDetails {

    /** Identifiant unique du mode de paiement (clé primaire). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom ou intitulé du moyen de paiement (ex. : virement, chèque, PayPal, etc.). */
    @NotBlank(message = "Le nom du moyen de paiement peut pas être null")
    private String paymentName;

    /**
     * Conditions de paiement (ex. : comptant, à 30 jours, etc.).
     * <p>Stocké sous forme d’énumération {@link PaymentTerms}.</p>
     * <p>Ce champ est obligatoire.</p>
     */
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Les termes de paiement doivent être défini")
    private PaymentTerms paymentTerm;

    /** Domiciliation bancaire associée au compte (ex. : nom de la banque). */
    @NotBlank(message = "La domiciliation peut pas être null")
    private String domiciliation;

    /** Nom du titulaire du compte bancaire. */
    @NotBlank(message = "Le nom du propriétaire du compte peut pas être null")
    private String holderName;

    /** Numéro IBAN du compte bancaire. */
    @NotBlank(message = "L'IBAN peut pas être null")
    private String iban;

    /** Code BIC/SWIFT associé au compte bancaire. */
    @NotBlank(message = "Le BIC peut pas être null")
    private String bic;

    /**
     * Utilisateur auquel ces informations de paiement appartiennent.
     * <p>Relation : un utilisateur peut posséder plusieurs {@link PaymentDetails}.</p>
     * <p>Ce champ est obligatoire.</p>
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Liste des factures utilisant ces informations de paiement.
     * <p>Relation : un mode de paiement peut être utilisé dans plusieurs {@link Invoice}.</p>
     * <p>L’annotation {@link JsonIgnore} empêche la sérialisation pour éviter les boucles JSON.</p>
     */
    @JsonIgnore
    @OneToMany(mappedBy = "paymentDetails")
    private List<Invoice> invoices;
}
