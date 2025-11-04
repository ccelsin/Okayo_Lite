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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final UserService userService;
    private final InvoiceRepository invoiceRepository;
    private final PdfInvoiceRenderer pdfInvoiceRenderer;

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + id));

        // S'assure que les totaux sont à jour avant rendu
        invoiceService.updateInvoiceTotals(invoice);

        byte[] pdf = pdfInvoiceRenderer.renderInvoicePage1(invoice);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("facture-" + invoice.getReference() + ".pdf")
                        .build()
        );
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<?> saveInvoice(HttpServletRequest request, @RequestBody InvoiceRequest invoiceRequest) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Only admins can create invoices.");
        }

        Long creatorId = userService.extractUserIdFromRequest(request);
        if (creatorId == null) {
            return ResponseUtils.unauthorized("Access denied");
        }

        try {
            InvoiceDto savedInvoice = invoiceService.saveInvoice(creatorId, invoiceRequest);
            return ResponseEntity.ok(savedInvoice);
        } catch (EntityNotFoundException ex) {
            return ResponseUtils.notFound(ex.getMessage());
        }
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<?> getAllInvoices(HttpServletRequest request) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Access denied");
        }
        List<InvoiceDto> invoices = invoiceService.getAllInvoices();
        return ResponseEntity.ok(invoices);
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<?> getInvoice(HttpServletRequest request, @PathVariable Long id) {
        if (userService.isAuthorized(request) == false) {
            return ResponseUtils.unauthorized("Access denied");
        }
        InvoiceDto invoice = invoiceService.getInvoice(id);
        if (invoice == null) {
            return ResponseUtils.notFound("Invoice not found with id " + id);
        }
        return ResponseEntity.ok(invoice);
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/mine")
    public ResponseEntity<?> getInvoiceOfCustomer(HttpServletRequest request, @PathVariable Long id) {
        if (userService.isAuthorized(request) == false) {
            return ResponseUtils.unauthorized("Access denied");
        }
        InvoiceDto invoice = invoiceService.getInvoice(id);
        if (invoice == null) {
            return ResponseUtils.notFound("Invoice not found with id " + id);
        }
        return ResponseEntity.ok(invoice);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<?> setInvoice(HttpServletRequest request, @PathVariable Long id, @RequestBody InvoiceUpdateRequest invoiceUpdateRequest) {
        if (userService.isAdmin(request) == false) {
            return ResponseUtils.forbidden("Only admins can update invoices.");
        }

        try {
            InvoiceDto updatedInvoice = invoiceService.setInvoice(id, invoiceUpdateRequest);
            return ResponseEntity.ok(updatedInvoice);
        } catch (EntityNotFoundException ex) {
            return ResponseUtils.notFound(ex.getMessage());
        }
    }
}