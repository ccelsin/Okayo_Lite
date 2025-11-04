package backend.dtos;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public record InvoiceDto(
    Long id,
    String reference,
    Date billingDate,
    Date dueDate,
    BigDecimal totalHT,
    BigDecimal totalTTC,
    Long customerId,
    Long creatorId,
    Long paymentDetailsId,
    List<Long> purchaseIds,
    boolean isConfirmed
) {
}