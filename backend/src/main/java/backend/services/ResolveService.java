package backend.services;

import org.springframework.stereotype.Service;

import backend.models.Invoice;
import backend.models.PaymentDetails;
import backend.models.Product;
import backend.models.Tva;
import backend.models.User;
import backend.repositories.InvoiceRepository;
import backend.repositories.PaymentDetailsRepository;
import backend.repositories.ProductRepository;
import backend.repositories.TvaRepository;
import backend.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service utilitaire responsable de la résolution des entités à partir de leurs identifiants.
 * 
 * <p>Ce service centralise la logique de récupération des entités persistées en base
 * (TVA, produit, facture, utilisateur, informations de paiement).  
 * Il garantit qu’une exception claire est levée si l’entité recherchée est introuvable.</p>
 *
 * <p>Annoté avec {@link Service}, ce composant est géré par le conteneur Spring.  
 * L’annotation {@link RequiredArgsConstructor} de Lombok permet l’injection automatique
 * des dépendances via un constructeur généré.</p>
 */
@Service
@RequiredArgsConstructor
public class ResolveService {

    /** Dépôt gérant les entités de type {@link Tva}. */
    private final TvaRepository tvaRepository;

    /** Dépôt gérant les entités de type {@link Product}. */
    private final ProductRepository productRepository;

    /** Dépôt gérant les entités de type {@link Invoice}. */
    private final InvoiceRepository invoiceRepository;

    /** Dépôt gérant les entités de type {@link User}. */
    private final UserRepository userRepository;

    /** Dépôt gérant les entités de type {@link PaymentDetails}. */
    private final PaymentDetailsRepository paymentDetailsRepository;

    /**
     * Récupère une entité {@link Tva} à partir de son identifiant.
     *
     * @param tvaId l’identifiant de la TVA à rechercher
     * @return la TVA correspondante
     * @throws EntityNotFoundException si l’identifiant est nul ou si la TVA n’existe pas
     */
    public Tva resolveTva(Long tvaId) {
        if (tvaId == null) {
            throw new EntityNotFoundException("L'id de la tva est nécessaire");
        }
        return tvaRepository.findById(tvaId)
            .orElseThrow(() -> new EntityNotFoundException("Tva " + tvaId + " introuvable"));
    }

    /**
     * Récupère un {@link Product} à partir de son identifiant.
     *
     * @param productId l’identifiant du produit à rechercher
     * @return le produit correspondant
     * @throws EntityNotFoundException si l’identifiant est nul ou si le produit n’existe pas
     */
    public Product resolveProduct(Long productId) {
        if (productId == null) {
            throw new EntityNotFoundException("L'id du produit est nécessaire");
        }
        return productRepository.findById(productId)
            .orElseThrow(() -> new EntityNotFoundException("Produit " + productId + " introuvable"));
    }

    /**
     * Récupère une {@link Invoice} à partir de son identifiant.
     *
     * @param invoiceId l’identifiant de la facture à rechercher
     * @return la facture correspondante
     * @throws EntityNotFoundException si l’identifiant est nul ou si la facture n’existe pas
     */
    public Invoice resolveInvoice(Long invoiceId) {
        if (invoiceId == null) {
            throw new EntityNotFoundException("L'id de la facture est nécessaire");
        }
        return invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Facture " + invoiceId + " introuvable"));
    }

    /**
     * Récupère un {@link PaymentDetails} à partir de son identifiant.
     *
     * @param paymentDetailsId l’identifiant des informations de paiement à rechercher
     * @return les informations de paiement correspondantes
     * @throws EntityNotFoundException si l’identifiant est nul ou si les informations n’existent pas
     */
    public PaymentDetails resolvePaymentDetails(Long paymentDetailsId) {
        if (paymentDetailsId == null) {
            throw new EntityNotFoundException("L'id des informations de paiement est nécessaire");
        }
        return paymentDetailsRepository.findById(paymentDetailsId)
            .orElseThrow(() -> new EntityNotFoundException("Information de paiement " + paymentDetailsId + " introuvable"));
    }

    /**
     * Récupère un {@link User} à partir de son identifiant.
     *
     * @param userId l’identifiant de l’utilisateur à rechercher
     * @return l’utilisateur correspondant
     * @throws EntityNotFoundException si l’identifiant est nul ou si l’utilisateur n’existe pas
     */
    public User resolveUser(Long userId) {
        if (userId == null) {
            throw new EntityNotFoundException("L'id de l'utilisateur est nécessaire");
        }
        return userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur " + userId + " introuvable"));
    }
}
