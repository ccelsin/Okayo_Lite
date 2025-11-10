package backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import backend.dtos.InvoiceDto;
import backend.dtos.InvoiceRequest;
import backend.models.Invoice;
import backend.models.PaymentDetails;
import backend.models.Purchase;
import backend.models.User;
import backend.repositories.InvoiceRepository;
import backend.repositories.PurchaseRepository;
import backend.services.ResolveService;
import backend.services.invoice.InvoiceService;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests unitaires (Mockito) de InvoiceService : totaux et création.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock InvoiceRepository invoiceRepository;
    @Mock ResolveService resolveService;
    @Mock PurchaseRepository purchaseRepository;

    @InjectMocks InvoiceService invoiceService;

    @Test
    void calculateTotalHT_et_TTC() {
        Invoice invoice = new Invoice();

        Purchase p1 = new Purchase();
        p1.setTotalHT(new BigDecimal("100.00"));
        p1.setTotalTva(new BigDecimal("20.00"));

        Purchase p2 = new Purchase();
        p2.setTotalHT(new BigDecimal("50.00"));
        p2.setTotalTva(new BigDecimal("10.00"));

        invoice.setPurchases(List.of(p1, p2));

        assertThat(invoiceService.calculateTotalHT(invoice)).isEqualByComparingTo("150.00");
        assertThat(invoiceService.calculateTotalTTC(invoice)).isEqualByComparingTo("180.00");
    }

    @Test
    void saveInvoice_initialise_reference_dates_totaux_et_liens() {
        Long creatorId = 1L;
        InvoiceRequest req = new InvoiceRequest(null, null, 2L, null, null, 3L);
        User creator = new User(); creator.setId(creatorId); creator.setUsername("admin");
        User customer = new User(); customer.setId(2L); customer.setUsername("bob");
        PaymentDetails pd = new PaymentDetails(); pd.setId(3L);

        when(resolveService.resolveUser(creatorId)).thenReturn(creator);
        when(resolveService.resolvePaymentDetails(3L)).thenReturn(pd);
        when(resolveService.resolveUser(2L)).thenReturn(customer);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceDto dto = invoiceService.saveInvoice(creatorId, req);

        assertThat(dto.reference()).isNotBlank();
        assertThat(dto.totalHT()).isZero();
        assertThat(dto.totalTTC()).isZero();
        assertThat(dto.creatorId()).isEqualTo(creatorId);
        assertThat(dto.customerId()).isEqualTo(2L);
        assertThat(dto.paymentDetailsId()).isEqualTo(3L);
    }
}
