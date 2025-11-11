package backend;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import backend.services.ResolveService;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitaires de {@link ResolveService}.
 *
 * Stratégie :
 *  - Chemins heureux : chaque méthode retourne l'entité quand l'ID est trouvé.
 *  - Erreurs : chaque méthode lève une EntityNotFoundException quand l'ID est null ou introuvable.
 *
 * Remarques :
 *  - Aucun contexte Spring n'est démarré (tests rapides et isolés).
 *  - Les dépôts sont mockés pour contrôler précisément les cas de figure.
 */
@ExtendWith(MockitoExtension.class)
class ResolveServiceTest {

    // Dépendances mockées
    @Mock private TvaRepository tvaRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentDetailsRepository paymentDetailsRepository;

    // Service testé
    @InjectMocks private ResolveService resolveService;

    // ---------------------------
    // Bloc TVA
    // ---------------------------
    @Nested
    @DisplayName("resolveTva")
    class ResolveTva {

        @Test
        @DisplayName("retourne la TVA quand l'ID existe")
        void returnsTva_whenFound() {
            Tva tva = new Tva();
            tva.setId(1L);
            when(tvaRepository.findById(1L)).thenReturn(Optional.of(tva));

            Tva result = resolveService.resolveTva(1L);

            assertThat(result).isSameAs(tva);
            verify(tvaRepository).findById(1L);
        }

        @Test
        @DisplayName("lève une exception quand l'ID est null")
        void throws_whenIdNull() {
            assertThatThrownBy(() -> resolveService.resolveTva(null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("tva");
            verifyNoInteractions(tvaRepository);
        }

        @Test
        @DisplayName("lève une exception quand l'entité est introuvable")
        void throws_whenEntityNotFound() {
            when(tvaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resolveService.resolveTva(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
            verify(tvaRepository).findById(99L);
        }
    }

    // ---------------------------
    // Bloc Product
    // ---------------------------
    @Nested
    @DisplayName("resolveProduct")
    class ResolveProduct {

        @Test
        @DisplayName("retourne le produit quand l'ID existe")
        void returnsProduct_whenFound() {
            Product p = new Product();
            p.setId(10L);
            when(productRepository.findById(10L)).thenReturn(Optional.of(p));

            Product result = resolveService.resolveProduct(10L);

            assertThat(result).isSameAs(p);
            verify(productRepository).findById(10L);
        }

        @Test
        @DisplayName("lève une exception quand l'ID est null")
        void throws_whenIdNull() {
            assertThatThrownBy(() -> resolveService.resolveProduct(null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("produit");
            verifyNoInteractions(productRepository);
        }

        @Test
        @DisplayName("lève une exception quand l'entité est introuvable")
        void throws_whenEntityNotFound() {
            when(productRepository.findById(123L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resolveService.resolveProduct(123L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("123");
            verify(productRepository).findById(123L);
        }
    }

    // ---------------------------
    // Bloc Invoice
    // ---------------------------
    @Nested
    @DisplayName("resolveInvoice")
    class ResolveInvoice {

        @Test
        @DisplayName("retourne la facture quand l'ID existe")
        void returnsInvoice_whenFound() {
            Invoice inv = new Invoice();
            inv.setId(5L);
            when(invoiceRepository.findById(5L)).thenReturn(Optional.of(inv));

            Invoice result = resolveService.resolveInvoice(5L);

            assertThat(result).isSameAs(inv);
            verify(invoiceRepository).findById(5L);
        }

        @Test
        @DisplayName("lève une exception quand l'ID est null")
        void throws_whenIdNull() {
            assertThatThrownBy(() -> resolveService.resolveInvoice(null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("facture");
            verifyNoInteractions(invoiceRepository);
        }

        @Test
        @DisplayName("lève une exception quand l'entité est introuvable")
        void throws_whenEntityNotFound() {
            when(invoiceRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resolveService.resolveInvoice(404L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("404");
            verify(invoiceRepository).findById(404L);
        }
    }

    // ---------------------------
    // Bloc PaymentDetails
    // ---------------------------
    @Nested
    @DisplayName("resolvePaymentDetails")
    class ResolvePaymentDetails {

        @Test
        @DisplayName("retourne les informations de paiement quand l'ID existe")
        void returnsPaymentDetails_whenFound() {
            PaymentDetails pd = new PaymentDetails();
            pd.setId(8L);
            when(paymentDetailsRepository.findById(8L)).thenReturn(Optional.of(pd));

            PaymentDetails result = resolveService.resolvePaymentDetails(8L);

            assertThat(result).isSameAs(pd);
            verify(paymentDetailsRepository).findById(8L);
        }

        @Test
        @DisplayName("lève une exception quand l'ID est null")
        void throws_whenIdNull() {
            assertThatThrownBy(() -> resolveService.resolvePaymentDetails(null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("paiement");
            verifyNoInteractions(paymentDetailsRepository);
        }

        @Test
        @DisplayName("lève une exception quand l'entité est introuvable")
        void throws_whenEntityNotFound() {
            when(paymentDetailsRepository.findById(777L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resolveService.resolvePaymentDetails(777L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("777");
            verify(paymentDetailsRepository).findById(777L);
        }
    }

    // ---------------------------
    // Bloc User
    // ---------------------------
    @Nested
    @DisplayName("resolveUser")
    class ResolveUser {

        @Test
        @DisplayName("retourne l'utilisateur quand l'ID existe")
        void returnsUser_whenFound() {
            User user = new User();
            user.setId(2L);
            when(userRepository.findById(2L)).thenReturn(Optional.of(user));

            User result = resolveService.resolveUser(2L);

            assertThat(result).isSameAs(user);
            verify(userRepository).findById(2L);
        }

        @Test
        @DisplayName("lève une exception quand l'ID est null")
        void throws_whenIdNull() {
            assertThatThrownBy(() -> resolveService.resolveUser(null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("utilisateur");
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("lève une exception quand l'entité est introuvable")
        void throws_whenEntityNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> resolveService.resolveUser(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("999");
            verify(userRepository).findById(999L);
        }
    }
}
