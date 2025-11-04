package backend.dtos;

import java.math.BigDecimal;
import java.util.Date;

public record InvoiceRequest(
    Date billingDate,
    Date dueDate,
    Long customerId,
    BigDecimal totalHT,
    BigDecimal totalTTC,
    Long paymentDetailsId
) {
}