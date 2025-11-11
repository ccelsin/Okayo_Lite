package backend.models;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Représente une configuration de taux de TVA (Taxe sur la Valeur Ajoutée).
 * 
 * <p>Cette entité permet de gérer différents taux de TVA applicables aux produits,
 * y compris les taux précédents, actuels et futurs. Elle prend également en compte
 * les périodes d’évolution des taux.</p>
 *
 * <p>La classe utilise l’annotation {@link Data} de Lombok pour générer automatiquement
 * les accesseurs, mutateurs, ainsi que les méthodes equals, hashCode et toString.</p>
 */
@Entity
@Table(name = "tva")
@Data
public class Tva {
    
    /** Identifiant unique de la configuration de TVA (clé primaire). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Ancien taux de TVA précédemment appliqué.
     * <p>Peut être nul si aucune évolution antérieure n’a eu lieu.</p>
     */
    @Column(nullable = true, precision = 4, scale = 2)
    private BigDecimal previousRate;

    /**
     * Taux de TVA actuellement appliqué.
     * <p>Ce champ est obligatoire et doit être défini pour tout enregistrement.</p>
     */
    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal defaultRate;

    /**
     * Taux de TVA qui sera appliqué à une date future.
     * <p>Peut être nul tant qu’aucune évolution n’est planifiée.</p>
     */
    @Column(nullable = true, precision = 4, scale = 2)
    private BigDecimal futureRate;

    /**
     * Date de début d’application du taux futur.
     * <p>Utilisée pour gérer les changements programmés de TVA.</p>
     */
    @Column(name = "start_evolution_date", nullable = true)
    private Date startEvolutionDate;

    /**
     * Date de fin d’application du taux futur.
     * <p>Permet de délimiter la période d’évolution du taux de TVA.</p>
     */
    @Column(name = "end_evolution_date", nullable = true)
    private Date endEvolutionDate;

    /**
     * Indique si l’évolution du taux de TVA a été appliquée.
     * <p>Valeur par défaut : {@code false}.</p>
     */
    private Boolean evolutionApplied = false;

    /**
     * Liste des produits associés à ce taux de TVA.
     * <p>Relation : une TVA peut être liée à plusieurs {@link Product}.</p>
     */
    @OneToMany(mappedBy = "tva")
    private List<Product> products;
}
