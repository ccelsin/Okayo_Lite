package backend.services.invoice;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import backend.dtos.InvoiceDto;
import backend.models.Invoice;
import backend.models.PaymentDetails;
import backend.models.Purchase;
import backend.models.User;

@Service
public class InvoiceMapperService {

    public static InvoiceDto toDto(Invoice invoice) {
        Long customerId = invoice.getCustomer() != null ? invoice.getCustomer().getId() : null;
        Long creatorId = invoice.getCreator() != null ? invoice.getCreator().getId() : null;
        Long paymentDetailsId = invoice.getPaymentDetails() != null ? invoice.getPaymentDetails().getId() : null;
        Boolean isConfirmed = Boolean.valueOf(invoice.isConfirmed());

        List<Long> purchaseIds = new ArrayList<>();
        if (invoice.getPurchases() != null) {
            for (Purchase purchase : invoice.getPurchases()) {
                if (purchase != null && purchase.getId() != null) {
                    purchaseIds.add(purchase.getId());
                }
            }
        }

        return new InvoiceDto(
            invoice.getId(),
            invoice.getReference(),
            invoice.getBillingDate(),
            invoice.getDueDate(),
            invoice.getTotalHT(),
            invoice.getTotalTTC(),
            customerId,
            creatorId,
            paymentDetailsId,
            purchaseIds,
            isConfirmed
        );
    }

    public static Invoice toEntity(InvoiceDto invoiceDto, User customer, User creator, PaymentDetails paymentDetails, List<Purchase> purchases) {
        Invoice invoice = new Invoice();
        invoice.setId(invoiceDto.id());
        invoice.setBillingDate(invoiceDto.billingDate());
        invoice.setDueDate(invoiceDto.dueDate());
        invoice.setTotalHT(invoiceDto.totalHT());
        invoice.setTotalTTC(invoiceDto.totalTTC());
        invoice.setCustomer(customer);
        invoice.setCreator(creator);
        invoice.setPaymentDetails(paymentDetails);
        invoice.setPurchases(purchases);
        return invoice;
    }

    public static List<InvoiceDto> toDtoList(List<Invoice> invoices) {
        List<InvoiceDto> invoiceDtos = new ArrayList<>();
        for (Invoice invoice : invoices) {
            invoiceDtos.add(toDto(invoice));
        }
        return invoiceDtos;
    }
}