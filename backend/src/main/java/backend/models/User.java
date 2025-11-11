package backend.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import backend.constants.UserRole;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

/**
 * Représente un utilisateur du système.
 * 
 * <p>Cette entité contient les informations d'identification et de contact d'un utilisateur,
 * ainsi que ses relations avec d'autres entités telles que les factures, les achats
 * et les détails de paiement.</p>
 *
 * <p>La classe utilise l'annotation {@link Data} de Lombok pour générer automatiquement
 * les accesseurs, mutateurs, méthodes equals, hashCode et toString.</p>
 */
@Entity
@Table(name = "users")
@Data
public class User {

    /** Identifiant unique de l'utilisateur (clé primaire). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom d'utilisateur utilisé pour l'authentification. */
    @NotBlank(message = "Le nom de l'utilisateur ne peut pas être null")
    private String username;

    /** Mot de passe de l'utilisateur (stocké de manière sécurisée). */
    @NotBlank(message = "Le mot de passe peut pas être null")
    private String password;

    /** Rôle de l'utilisateur dans l'application (ex. ADMIN, CUSTOMER, etc.). */
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Le rôle peut pas être null")
    private UserRole role;

    /** Adresse e-mail de l'utilisateur (facultative, doit respecter le format e-mail). */
    @Email(message="Le format doit être conforme à celui d'un mail")
    @Nullable
    private String email;

    /** Numéro de téléphone de l'utilisateur (facultatif). */
    @Nullable
    private String phoneNumber;

    /** Code client associé à l'utilisateur (le cas échéant). */
    private String codeCustomer;

    /** Adresse postale de l'utilisateur (facultative). */
    @Nullable
    private String address;

    /** Code postal associé à l'adresse (facultatif). */
    @Nullable
    private String postalCode;

    /** Ville de résidence ou de l’entreprise de l’utilisateur (facultative). */
    @Nullable
    private String city;

    /** Site web de l’utilisateur ou de son entreprise (facultatif). */
    @Nullable
    private String website;

    /**
     * Liste des informations de paiement associées à cet utilisateur.
     * <p>Relation : Un utilisateur peut avoir plusieurs enregistrements de {@link PaymentDetails}.</p>
     */
    @OneToMany(mappedBy = "user")
    @Nullable
    private List<PaymentDetails> paymentDetails;

    /**
     * Liste des factures créées par cet utilisateur.
     * <p>Relation : Un utilisateur peut créer plusieurs {@link Invoice}.</p>
     * <p>Annotation {@link JsonIgnore} : empêche la sérialisation pour éviter les boucles infinies JSON.</p>
     */
    @JsonIgnore
    @OneToMany(mappedBy = "creator")
    @Nullable
    private List<Invoice> invoices;

    /**
     * Liste des factures où cet utilisateur est enregistré comme client.
     * <p>Relation : Un utilisateur peut être le client de plusieurs {@link Invoice}.</p>
     */
    @JsonIgnore
    @OneToMany(mappedBy = "customer")
    @Nullable
    private List<Invoice> customerInvoices;

    /**
     * Liste des achats effectués par l'utilisateur en tant qu'acheteur.
     * <p>Relation : Un utilisateur peut avoir plusieurs {@link Purchase}.</p>
     */
    @JsonIgnore
    @OneToMany(mappedBy = "purchaser")
    @Nullable
    private List<Purchase> customerPurchases;
    
}
