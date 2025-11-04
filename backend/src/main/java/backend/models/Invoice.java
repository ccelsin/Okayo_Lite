package backend.models;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;
@Entity
@Table(name = "invoices")
@Data
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String reference;

    @Nullable
    private Date billingDate;

    @Nullable
    private Date dueDate;

    private BigDecimal totalHT;

    private BigDecimal totalTTC;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = true)
    private User customer;

    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User creator;


    @JsonIgnore
    @OneToMany(mappedBy = "invoice")
    private List<Purchase> purchases;

    @ManyToOne
    @JoinColumn(name = "payment_details_id", nullable = false)
    private PaymentDetails paymentDetails;

    @Column(nullable = false)
    private boolean isConfirmed = false;

    
}
