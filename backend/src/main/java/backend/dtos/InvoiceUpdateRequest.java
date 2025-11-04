package backend.dtos;

import java.util.Date;

public record InvoiceUpdateRequest(
    Date billingDate,
    Date dueDate,
    Long customerId,
    boolean isConfirmed
) {
}