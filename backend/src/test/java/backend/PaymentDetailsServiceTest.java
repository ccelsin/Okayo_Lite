package backend;

import backend.constants.PaymentTerms;
import backend.dtos.PaymentDetailsDto;
import backend.dtos.PaymentDetailsRequest;
import backend.models.PaymentDetails;
import backend.models.User;
import backend.repositories.PaymentDetailsRepository;
import backend.repositories.UserRepository;
import backend.services.paymentdetails.PaymentDetailsService;
import jakarta.persistence.EntityNotFoundException;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du service PaymentDetailsService.
 *
 * Ces tests vérifient :
 *  - la création de nouvelles informations de paiement (cas nominal et utilisateur inexistant),
 *  - la récupération par ID (valide, inexistant, ou ID nul),
 *  - la récupération de toutes les informations,
 *  - la mise à jour d’informations existantes (cas nominal et erreurs).
 */
@ExtendWith(MockitoExtension.class)
class PaymentDetailsServiceTest {

    @Mock
    private PaymentDetailsRepository paymentDetailsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PaymentDetailsService service;

    @Test
    @DisplayName("savePaymentDetails : crée une information de paiement valide")
    void savePaymentDetails_cree_valide() {
        // Arrange
        Long userId = 10L;
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        PaymentDetailsRequest request = new PaymentDetailsRequest(
                "Virement",
                PaymentTerms.TOTAL_TTC,
                "BNP Paris",
                "Dupont Jean",
                "FR761234567890",
                "BIC12345"
        );

        PaymentDetails savedEntity = new PaymentDetails();
        savedEntity.setId(50L);
        savedEntity.setPaymentName("Virement");
        savedEntity.setHolderName("Dupont Jean");
        savedEntity.setUser(user);

        when(paymentDetailsRepository.save(any(PaymentDetails.class))).thenReturn(savedEntity);

        // Act
        PaymentDetailsDto result = service.savePaymentDetails(userId, request);

        // Assert
        assertThat(result.id()).isEqualTo(50L);
        assertThat(result.paymentName()).isEqualTo("Virement");
        assertThat(result.holderName()).isEqualTo("Dupont Jean");
        assertThat(result.userId()).isEqualTo(userId);

        verify(userRepository).findById(userId);
        verify(paymentDetailsRepository).save(any(PaymentDetails.class));
    }

    @Test
    @DisplayName("savePaymentDetails : lève EntityNotFoundException si l'utilisateur n'existe pas")
    void savePaymentDetails_utilisateur_introuvable() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        PaymentDetailsRequest request = new PaymentDetailsRequest(
                "CB",
                PaymentTerms.TOTAL_TTC,
                "BanqueX",
                "Martin",
                "FR123",
                "BIC"
        );

        // Act & Assert
        assertThatThrownBy(() -> service.savePaymentDetails(99L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Utilisateur introuvable");

        verify(userRepository).findById(99L);
        verifyNoInteractions(paymentDetailsRepository);
    }

    @Test
    @DisplayName("getPaymentDetails : retourne un DTO si trouvé")
    void getPaymentDetails_retourne_dto_si_trouve() throws Exception {
        PaymentDetails pd = new PaymentDetails();
        pd.setId(5L);
        pd.setPaymentName("CB");

        when(paymentDetailsRepository.findById(5L)).thenReturn(Optional.of(pd));

        PaymentDetailsDto result = service.getPaymentDetails(5L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.paymentName()).isEqualTo("CB");

        verify(paymentDetailsRepository).findById(5L);
    }

    @Test
    @DisplayName("getPaymentDetails : retourne null si non trouvé")
    void getPaymentDetails_retourne_null_si_absent() throws Exception {
        when(paymentDetailsRepository.findById(50L)).thenReturn(Optional.empty());

        PaymentDetailsDto result = service.getPaymentDetails(50L);

        assertThat(result).isNull();
        verify(paymentDetailsRepository).findById(50L);
    }

    @Test
    @DisplayName("getPaymentDetails : lève BadRequestException si id est null")
    void getPaymentDetails_id_null_declenche_exception() {
        assertThatThrownBy(() -> service.getPaymentDetails(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("valide");

        verifyNoInteractions(paymentDetailsRepository);
    }

    @Test
    @DisplayName("getAllPaymentDetails : retourne la liste des DTOs")
    void getAllPaymentDetails_ok() {
        PaymentDetails pd1 = new PaymentDetails();
        pd1.setId(1L);
        PaymentDetails pd2 = new PaymentDetails();
        pd2.setId(2L);

        when(paymentDetailsRepository.findAll()).thenReturn(List.of(pd1, pd2));

        List<PaymentDetailsDto> result = service.getAllPaymentDetails();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(1).id()).isEqualTo(2L);

        verify(paymentDetailsRepository).findAll();
    }

    @Test
    @DisplayName("setPaymentDetails : met à jour une information existante")
    void setPaymentDetails_met_a_jour_valide() {
        // Arrange
        Long userId = 5L;
        User user = new User();
        user.setId(userId);

        PaymentDetails existing = new PaymentDetails();
        existing.setId(8L);
        existing.setPaymentName("AncienNom");
        existing.setHolderName("AncienTitulaire");

        PaymentDetails updated = new PaymentDetails();
        updated.setId(8L);
        updated.setPaymentName("NouveauNom");
        updated.setHolderName("NouveauTitulaire");
        updated.setUser(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(paymentDetailsRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(paymentDetailsRepository.save(any(PaymentDetails.class))).thenReturn(updated);

        PaymentDetailsDto dto = new PaymentDetailsDto(
                8L,
                "NouveauNom",
                PaymentTerms.TOTAL_HT,
                "DomiciliationX",
                "NouveauTitulaire",
                "IBAN999",
                "BICNEW",
                userId
        );

        // Act
        PaymentDetailsDto result = service.setPaymentDetails(userId, dto);

        // Assert
        assertThat(result.id()).isEqualTo(8L);
        assertThat(result.paymentName()).isEqualTo("NouveauNom");
        assertThat(result.holderName()).isEqualTo("NouveauTitulaire");
        assertThat(result.userId()).isEqualTo(userId);

        verify(userRepository).findById(userId);
        verify(paymentDetailsRepository).findById(8L);
        verify(paymentDetailsRepository).save(any(PaymentDetails.class));
    }

    @Test
    @DisplayName("setPaymentDetails : lève EntityNotFoundException si l'utilisateur est introuvable")
    void setPaymentDetails_utilisateur_introuvable() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        PaymentDetailsDto dto = new PaymentDetailsDto(
                1L, "Nom", PaymentTerms.TOTAL_TTC, "Dom", "Titulaire", "IBAN", "BIC", 9L
        );

        assertThatThrownBy(() -> service.setPaymentDetails(9L, dto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Utilisateur introuvable");

        verify(userRepository).findById(9L);
        verifyNoInteractions(paymentDetailsRepository);
    }

    @Test
    @DisplayName("setPaymentDetails : lève EntityNotFoundException si l'information de paiement est introuvable")
    void setPaymentDetails_paymentdetails_introuvable() {
        Long userId = 10L;
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(paymentDetailsRepository.findById(50L)).thenReturn(Optional.empty());

        PaymentDetailsDto dto = new PaymentDetailsDto(
                50L, "Nom", PaymentTerms.TOTAL_TTC, "Dom", "Titulaire", "IBAN", "BIC", userId
        );

        assertThatThrownBy(() -> service.setPaymentDetails(userId, dto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Information de paiement introuvable");

        verify(userRepository).findById(userId);
        verify(paymentDetailsRepository).findById(50L);
        verify(paymentDetailsRepository, never()).save(any());
    }
}
