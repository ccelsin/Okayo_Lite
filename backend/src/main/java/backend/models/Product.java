package backend.models;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Représente un produit disponible à l’achat dans le système.
 * 
 * <p>Cette entité contient les informations essentielles d’un produit,
 * notamment son nom, son prix unitaire hors taxe et le taux de TVA associé.
 * Elle est également liée aux différents achats où le produit intervient.</p>
 *
 * <p>L’annotation {@link Data} de Lombok permet de générer automatiquement
 * les accesseurs, mutateurs, ainsi que les méthodes equals, hashCode et toString.</p>
 */
@Entity
@Table(name = "products")
@Data
public class Product {
    
    /** Identifiant unique du produit (clé primaire). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom du produit (obligatoire, ne peut pas être nul ou vide). */
    @NotBlank(message = "Le nom peut pas être null ")
    private String name;

    /**
     * Prix unitaire hors taxe du produit.
     * <p>Doit être un nombre positif.</p>
     * <p>Précision : 15 chiffres au total, dont 2 décimales.</p>
     */
    @Positive
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPriceHT;

    /**
     * Taux de TVA applicable à ce produit.
     * <p>Relation : plusieurs produits peuvent partager le même taux de {@link Tva}.</p>
     * <p>Ce champ est obligatoire.</p>
     */
    @ManyToOne
    @JoinColumn(name = "tva_id", nullable = false)
    private Tva tva;

    /**
     * Liste des achats associés à ce produit.
     * <p>Relation : un produit peut apparaître dans plusieurs {@link Purchase}.</p>
     */
    @OneToMany(mappedBy = "product")
    private List<Purchase> purchases;
}
