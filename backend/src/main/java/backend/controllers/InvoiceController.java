package backend.controllers;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.dtos.InvoiceDto;
import backend.dtos.InvoiceRequest;
import backend.dtos.InvoiceUpdateRequest;
import backend.models.Invoice;
import backend.repositories.InvoiceRepository;
import backend.services.PdfInvoiceRenderer;
import backend.services.invoice.InvoiceService;
import backend.services.user.UserService;
import backend.utilities.ResponseUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur REST responsable de la gestion des factures ({@link backend.models.Invoice}).
 *
 * <p>Ce contrôleur fournit des endpoints pour :</p>
 * <ul>
 *   <li>Générer et télécharger une facture au format PDF,</li>
 *   <li>Créer une nouvelle facture,</li>
 *   <li>Afficher une ou plusieurs factures,</li>
 *   <li>Mettre à jour le statut ou les informations d’une facture existante.</li>
 * </ul>
 *
 * <p>Les routes sont sécurisées via JWT et nécessitent un rôle administrateur ou utilisateur selon le cas.
 * Les réponses HTTP sont homogènes et gérées par la classe utilitaire {@link ResponseUtils}.</p>
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invoices")
public class InvoiceController {

    /** Service métier gérant la logique de création, lecture et mise à jour des factures. */
    private final InvoiceService invoiceService;

    /** Service gérant l’authentification et la vérification des rôles utilisateur. */
    private final UserService userService;

    /** Accès direct au dépôt de factures pour certaines opérations spécifiques. */
    private final InvoiceRepository invoiceRepository;

    /** Service utilitaire pour le rendu des factures au format PDF. */
    private final PdfInvoiceRenderer pdfInvoiceRenderer;

    /**
     * Génère et télécharge une facture au format PDF à partir de son identifiant.
     *
     * <p>Cette méthode :
     * <ul>
     *   <li>récupère la facture en base,</li>
     *   <li>met à jour les totaux si nécessaire,</li>
     *   <li>génère un PDF à l’aide du service {@link PdfInvoiceRenderer},</li>
     *   <li>et renvoie le fichier en pièce jointe.</li>
     * </ul></p>
     *
     * @param id identifiant unique de la facture
     * @return le fichier PDF de la facture sous forme de flux binaire
     * @throws EntityNotFoundException si la facture n’existe pas
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Facture " + id + " introuvable"));

        // S’assure que les totaux sont à jour avant le rendu PDF
        invoiceService.updateInvoiceTotals(invoice);

        byte[] pdf = pdfInvoiceRenderer.renderInvoicePage1(invoice);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("Facture-" + invoice.getReference() + ".pdf")
                        .build()
        );
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    /**
     * Crée une nouvelle facture dans le système.
     *
     * <p>Accessible uniquement aux administrateurs.
     * L’identifiant du créateur est extrait du token JWT.
     * En cas d’erreur (client ou mode de paiement inexistant), une réponse adaptée est renvoyée.</p>
     *
     * @param request la requête HTTP contenant le token JWT
     * @param invoiceRequest les données nécessaires à la création de la facture
     * @return la facture créée sous forme de {@link InvoiceDto}
     */
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<?> saveInvoice(HttpServletRequest request, @RequestBody InvoiceRequest invoiceRequest) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Seuls les administrateurs peuvent créer des factures");
        }

        Long creatorId = userService.extractUserIdFromRequest(request);
        if (creatorId == null) {
            return ResponseUtils.unauthorized("Accès non autorisé");
        }

        try {
            InvoiceDto savedInvoice = invoiceService.saveInvoice(creatorId, invoiceRequest);
            return ResponseEntity.ok(savedInvoice);
        } catch (EntityNotFoundException ex) {
            return ResponseUtils.notFound(ex.getMessage());
        }
    }

    /**
     * Récupère la liste complète de toutes les factures existantes.
     *
     * <p>Accessible uniquement aux administrateurs.</p>
     *
     * @param request la requête HTTP contenant le token JWT
     * @return une liste d’objets {@link InvoiceDto}
     */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<?> getAllInvoices(HttpServletRequest request) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Accès refusé");
        }
        List<InvoiceDto> invoices = invoiceService.getAllInvoices();
        return ResponseEntity.ok(invoices);
    }

    /**
     * Récupère les détails d’une facture spécifique par son identifiant.
     *
     * <p>Accessible à tout utilisateur authentifié (client ou administrateur).
     * Si la facture n’existe pas, une erreur 404 est renvoyée.</p>
     *
     * @param request la requête HTTP contenant le token JWT
     * @param id l’identifiant de la facture recherchée
     * @return les détails de la facture ou un message d’erreur
     */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<?> getInvoice(HttpServletRequest request, @PathVariable Long id) {
        if (userService.isAuthorized(request) == false) {
            return ResponseUtils.unauthorized("Accès refusé");
        }
        InvoiceDto invoice = invoiceService.getInvoice(id);
        if (invoice == null) {
            return ResponseUtils.notFound("Facture introuvable avec l'identifiant " + id);
        }
        return ResponseEntity.ok(invoice);
    }

    /**
     * Récupère toutes les factures confirmées appartenant au client connecté.
     *
     * <p>Accessible aux clients et aux administrateurs.
     * Les factures retournées sont limitées à celles du client authentifié.</p>
     *
     * @param request la requête HTTP contenant le token JWT
     * @return une liste des factures du client connecté
     */
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/mine")
    public ResponseEntity<?> getInvoicesOfCustomer(HttpServletRequest request) {
        if (userService.isAuthorized(request) == false) {
            return ResponseUtils.unauthorized("Accès refusé");
        }
        Long customerId = userService.extractUserIdFromRequest(request);
        List<InvoiceDto> invoice = invoiceService.getInvoicesOfCustomer(customerId);
        if (invoice == null) {
            return ResponseUtils.notFound("Aucune facture trouvée pour ce client");
        }
        return ResponseEntity.ok(invoice);
    }

    /**
     * Met à jour une facture existante (ex. confirmation ou modification des dates).
     *
     * <p>Accessible uniquement aux administrateurs.
     * Si la facture n’existe pas, une erreur 404 est renvoyée.</p>
     *
     * @param request la requête HTTP contenant le token JWT
     * @param id l’identifiant de la facture à modifier
     * @param invoiceUpdateRequest les nouvelles informations de la facture
     * @return la facture mise à jour sous forme de {@link InvoiceDto}
     */
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<?> setInvoice(HttpServletRequest request, @PathVariable Long id, @RequestBody InvoiceUpdateRequest invoiceUpdateRequest) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Seuls les administrateurs peuvent modifier des factures");
        }

        try {
            InvoiceDto updatedInvoice = invoiceService.setInvoice(id, invoiceUpdateRequest);
            return ResponseEntity.ok(updatedInvoice);
        } catch (EntityNotFoundException ex) {
            return ResponseUtils.notFound(ex.getMessage());
        }
    }
}
