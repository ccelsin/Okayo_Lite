package backend.services.invoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;

import backend.dtos.InvoiceDto;
import backend.dtos.InvoiceRequest;
import backend.dtos.InvoiceUpdateRequest;
import backend.models.Invoice;
import backend.models.PaymentDetails;
import backend.models.Purchase;
import backend.models.User;
import backend.repositories.InvoiceRepository;
import backend.repositories.PurchaseRepository;
import backend.services.ResolveService;
import backend.utilities.BeanCopyUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ResolveService resolveService;
    private final PurchaseRepository purchaseRepository;

    //Create a invoice
    public InvoiceDto saveInvoice(Long creatorId, InvoiceRequest invoiceRequest) {
        User creator = resolveService.resolveUser(creatorId);
        PaymentDetails paymentDetails = resolveService.resolvePaymentDetails(invoiceRequest.paymentDetailsId());

        Invoice invoice = new Invoice();
        invoice.setReference(generateReference());
        invoice.setBillingDate(invoiceRequest.billingDate());
        invoice.setCustomer(resolveService.resolveUser(invoiceRequest.customerId()));
        invoice.setDueDate(invoiceRequest.dueDate());
        invoice.setCreator(creator);
        invoice.setPaymentDetails(paymentDetails);

        // Initialise to zero
        invoice.setTotalHT(BigDecimal.ZERO);
        invoice.setTotalTTC(BigDecimal.ZERO);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return InvoiceMapperService.toDto(savedInvoice);
    }

    // Calculate the total tva using the purchase in the invoice
    public BigDecimal calculateTotalHT(Invoice invoice) {
        if (invoice.getPurchases() == null || invoice.getPurchases().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return invoice.getPurchases().stream()
                .map(Purchase::getTotalHT)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalTTC(Invoice invoice) {
        if (invoice.getPurchases() == null || invoice.getPurchases().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return invoice.getPurchases().stream()
                .map(p -> {
                    if (p.getTotalHT() == null || p.getTotalTva() == null) {
                        return BigDecimal.ZERO;
                    }
                    return p.getTotalHT().add(p.getTotalTva());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Update the invoice totals after adding purchase
    public Invoice updateInvoiceTotals(Invoice invoice) {
        BigDecimal totalHT = calculateTotalHT(invoice);
        BigDecimal totalTTC = calculateTotalTTC(invoice);

        invoice.setTotalHT(totalHT);
        invoice.setTotalTTC(totalTTC);

        return invoiceRepository.save(invoice);
    }

    
    public InvoiceDto getInvoice(Long id) {
        return invoiceRepository.findById(id)
                .map(InvoiceMapperService::toDto)
                .orElse(null);
    }

    public List<InvoiceDto> getAllInvoices() {
    List<Invoice> invoices = invoiceRepository.findAll();

    // Update invoice not confirmed
    for (Invoice invoice : invoices) {
        if(invoice.isConfirmed() == false){
            updateInvoiceTotals(invoice);
        }
        updateInvoiceTotals(invoice);
    }

    // Convertit en DTOs
    return InvoiceMapperService.toDtoList(invoices);
}

    public List<InvoiceDto> getInvoicesOfCustomer(Long id) {
        List<Invoice> invoices = invoiceRepository.findByCustomerIdAndIsConfirmedTrue(id);
        return InvoiceMapperService.toDtoList(invoices);
    }

    public InvoiceDto setInvoice(Long id, InvoiceUpdateRequest invoiceUpdateRequest) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found with id " + id));
        if (invoiceUpdateRequest.isConfirmed()) {
        // Confirm every purchase
            if (invoice.getPurchases() != null && !invoice.getPurchases().isEmpty()) {
                invoice.getPurchases().forEach(p -> p.setConfirmed(true));
                // Save the purchase
                purchaseRepository.saveAll(invoice.getPurchases());
                BeanCopyUtils.copyNonNullProperties(invoiceUpdateRequest, invoice);
                Invoice savedInvoice = invoiceRepository.save(invoice);
                return InvoiceMapperService.toDto(savedInvoice);
            }
        }
        if (invoiceUpdateRequest.billingDate() != null) {
            invoice.setBillingDate(invoiceUpdateRequest.billingDate());
        }
        if (invoiceUpdateRequest.dueDate() != null) {
            invoice.setDueDate(invoiceUpdateRequest.dueDate());
        }

        // Recalculate totals
        updateInvoiceTotals(invoice);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return InvoiceMapperService.toDto(savedInvoice);
    }

    // Generate reference of invoice
    private String generateReference() {
        int year = LocalDate.now().getYear();
        String reference;
        do {
            String suffix = String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
            reference = year + "-" + suffix;
        } while (invoiceRepository.findByReference(reference).isPresent());
        return reference;
    }
}
