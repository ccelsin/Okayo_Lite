package backend.models;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Représente un achat effectué par un utilisateur.
 * 
 * <p>Cette entité relie un produit acheté à son acheteur (utilisateur) et à une facture éventuelle.
 * Elle stocke les informations financières liées à l’achat, telles que le prix unitaire, le total
 * hors taxe, la TVA appliquée et le total de la TVA.</p>
 *
 * <p>La classe utilise l’annotation {@link Data} de Lombok pour générer automatiquement
 * les accesseurs, mutateurs, et méthodes utilitaires (equals, hashCode, toString).</p>
 */
@Entity
@Table(name = "purchases")
@Data
public class Purchase {

    /** Identifiant unique de l’achat (clé primaire). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Produit concerné par l’achat.
     * <p>Relation : plusieurs achats peuvent concerner le même {@link Product}.</p>
     * <p>Ce champ est obligatoire.</p>
     */
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Nom ou description de l’achat (obligatoire). */
    @NotBlank(message = "Le nom peut pas être null")
    private String name;
    
    /**
     * Quantité achetée du produit.
     * <p>Doit être positive et non nulle.</p>
     */
    @NotNull(message = "La quantité peut pas être null")
    @Positive(message = "La quantité doit être positive")
    private BigDecimal quantity;

    /**
     * Prix unitaire hors taxe du produit.
     * <p>Précision : 15 chiffres au total, dont 2 décimales.</p>
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPriceHT;

    /**
     * Montant total hors taxe (HT) de l’achat.
     * <p>Calculé en général comme : {@code quantity × unitPriceHT}.</p>
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalHT;

    /**
     * Taux de TVA appliqué à cet achat.
     * <p>Exprimé en pourcentage (ex. 20.00 pour 20%).</p>
     */
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal tvaApplied;

    /**
     * Montant total de la TVA calculé pour cet achat.
     * <p>Calculé en général comme : {@code totalHT × (tvaApplied / 100)}.</p>
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalTva;

    /**
     * Facture à laquelle l’achat est rattaché (le cas échéant).
     * <p>Annotation {@link JsonIgnore} : empêche la sérialisation JSON pour éviter
     * les références circulaires.</p>
     */
    @JsonIgnore
    @ManyToOne
    @Nullable
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    /**
     * Utilisateur ayant effectué l’achat.
     * <p>Relation : plusieurs achats peuvent être associés au même {@link User}.</p>
     * <p>Ce champ est obligatoire.</p>
     */
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User purchaser;

    /**
     * Indique si l’achat a été confirmé.
     * <p>Valeur par défaut : {@code false}.</p>
     */
    @Column(nullable = false)
    private boolean isConfirmed = false;
}
