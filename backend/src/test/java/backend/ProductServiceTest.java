package backend;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import backend.dtos.ProductDto;
import backend.models.Product;
import backend.models.Tva;
import backend.repositories.ProductRepository;
import backend.services.ResolveService;
import backend.services.product.ProductService;
import jakarta.persistence.EntityNotFoundException;

/**
 * Tests unitaires de {@link ProductService}.
 *
 * Objectifs :
 *  - Vérifier la création (validation des champs, résolution TVA, mapping entité/DTO).
 *  - Vérifier la lecture d’un produit par id et la lecture de tous les produits.
 *  - Vérifier la mise à jour (copie des champs non nuls + mise à jour de la relation TVA).
 *  - Vérifier les erreurs : BadRequest (champs manquants) et EntityNotFound (produit introuvable).
 *
 * Remarques :
 *  - Aucun contexte Spring n’est démarré : tests rapides et isolés.
 *  - On stubbe systématiquement resolveTva(...) avec un ID non nul pour éviter les faux positifs.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ResolveService resolveService;

    @InjectMocks private ProductService service;

    // ------------------------------------------------------------------------------------
    // saveProductDetails
    // ------------------------------------------------------------------------------------

    @Test
    @DisplayName("saveProductDetails : crée un produit quand les champs requis sont présents")
    void saveProductDetails_cree_quand_valide() throws Exception {
    // Arrange
    Long tvaId = 2L;

    // TVA résolue (ID non nul)
    Tva tva = new Tva();
    tva.setId(tvaId);

    when(resolveService.resolveTva(tvaId)).thenReturn(tva);

    // Capture de l’entité persistée
    ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);

    
    when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
        Product p = inv.getArgument(0);
        p.setId(100L);
        return p;
    });

    ProductDto input = new ProductDto(
        null,
        "Clavier mécanique",
        new BigDecimal("79.90"),
        tvaId
    );

    // Act
    ProductDto out = service.saveProductDetails(input);

    // Assert (DTO de sortie)
    assertThat(out.id()).isEqualTo(100L);
    assertThat(out.name()).isEqualTo("Clavier mécanique");
    assertThat(out.unitPriceHT()).isEqualByComparingTo("79.90");
    assertThat(out.tvaId()).isEqualTo(2L);

    // Assert (entité envoyée au repository)
    verify(productRepository).save(captor.capture());
    Product saved = captor.getValue();

    
    assertThat(saved.getName()).isEqualTo("Clavier mécanique");
    assertThat(saved.getUnitPriceHT()).isEqualByComparingTo("79.90");
    assertThat(saved.getTva()).isSameAs(tva);

    verify(resolveService).resolveTva(2L);
    verifyNoMoreInteractions(resolveService, productRepository);
}


    @Test
    @DisplayName("saveProductDetails : lève BadRequestException si le nom est manquant (tvaId présent)")
    void saveProductDetails_badrequest_si_nom_manquant() {
        // Arrange : tvaId présent => resolveTva est appelé AVANT la validation
        Long tvaId = 5L;
        Tva tva = new Tva(); tva.setId(tvaId);
        when(resolveService.resolveTva(tvaId)).thenReturn(tva);

        ProductDto input = new ProductDto(
            null,
            null,                      // nom manquant
            new BigDecimal("10.00"),
            tvaId
        );

        // Act + Assert
        assertThatThrownBy(() -> service.saveProductDetails(input))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("valide");

        verify(resolveService).resolveTva(tvaId);
        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("saveProductDetails : lève BadRequestException si le prix est manquant (tvaId présent)")
    void saveProductDetails_badrequest_si_prix_manquant() {
        Long tvaId = 3L;
        Tva tva = new Tva(); tva.setId(tvaId);
        when(resolveService.resolveTva(tvaId)).thenReturn(tva);

        ProductDto input = new ProductDto(
            null,
            "Ecran 27",
            null,                      // prix manquant
            tvaId
        );

        assertThatThrownBy(() -> service.saveProductDetails(input))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("valide");

        verify(resolveService).resolveTva(tvaId);
        verifyNoInteractions(productRepository);
    }

    //  Si tvaId est null, le service résout d’abord la TVA et lèvera plutôt EntityNotFoundException
    // via ResolveService. On reproduit ce comportement ici :
    @Test
    @DisplayName("saveProductDetails : tvaId null -> échec de résolution TVA (EntityNotFoundException)")
    void saveProductDetails_tva_null_declenche_resolve_exception() {
        ProductDto input = new ProductDto(
            null,
            "Article",
            new BigDecimal("12.00"),
            null // tvaId null -> resolveTva(null) lève une EntityNotFoundException dans l'implémentation actuelle
        );

        when(resolveService.resolveTva(null)).thenThrow(new EntityNotFoundException("id TVA requis"));

        assertThatThrownBy(() -> service.saveProductDetails(input))
            .isInstanceOf(EntityNotFoundException.class);

        verify(resolveService).resolveTva(null);
        verifyNoInteractions(productRepository);
    }

    // ------------------------------------------------------------------------------------
    // getProductDetails
    // ------------------------------------------------------------------------------------

    @Test
    @DisplayName("getProductDetails : retourne un DTO si trouvé, sinon null")
    void getProductDetails_ok_ou_null() {
        Product p = new Product();
        p.setId(10L);
        p.setName("Souris");
        p.setUnitPriceHT(new BigDecimal("19.99"));
        Tva t = new Tva(); t.setId(7L);
        p.setTva(t);

        when(productRepository.findById(10L)).thenReturn(Optional.of(p));
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        var dto = service.getProductDetails(10L);
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.name()).isEqualTo("Souris");
        assertThat(dto.unitPriceHT()).isEqualByComparingTo("19.99");
        assertThat(dto.tvaId()).isEqualTo(7L);

        assertThat(service.getProductDetails(404L)).isNull();

        verify(productRepository).findById(10L);
        verify(productRepository).findById(404L);
    }

    // ------------------------------------------------------------------------------------
    // getAllProductDetails
    // ------------------------------------------------------------------------------------

    @Test
    @DisplayName("getAllProductDetails : mappe la liste des entités vers des DTO")
    void getAllProductDetails_ok() {
        Product p1 = new Product(); p1.setId(1L);
        Product p2 = new Product(); p2.setId(2L);
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        var list = service.getAllProductDetails();

        assertThat(list).hasSize(2);
        assertThat(list.get(0).id()).isEqualTo(1L);
        assertThat(list.get(1).id()).isEqualTo(2L);
        verify(productRepository).findAll();
    }

    // ------------------------------------------------------------------------------------
    // setProductDetails
    // ------------------------------------------------------------------------------------

    @Test
    @DisplayName("setProductDetails : met à jour les champs non nuls et la relation TVA (si tvaId présent)")
    void setProductDetails_met_a_jour_champs_et_tva() {
        // Entité existante
        Product existing = new Product();
        existing.setId(50L);
        existing.setName("Ancien nom");
        existing.setUnitPriceHT(new BigDecimal("9.99"));

        when(productRepository.findById(50L)).thenReturn(Optional.of(existing));

        // Nouvelle TVA à lier
        Long newTvaId = 8L;
        Tva newTva = new Tva(); newTva.setId(newTvaId);
        when(resolveService.resolveTva(newTvaId)).thenReturn(newTva);

        // Capturer l’entité sauvegardée
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // DTO partiel : on met à jour le nom, le prix et la TVA
        ProductDto update = new ProductDto(
            50L,
            "Nouveau nom",
            new BigDecimal("12.49"),
            newTvaId
        );

        // Act
        ProductDto out = service.setProductDetails(update);

        // Assert : champs remplacés et TVA reliée
        assertThat(out.id()).isEqualTo(50L);
        assertThat(out.name()).isEqualTo("Nouveau nom");
        assertThat(out.unitPriceHT()).isEqualByComparingTo("12.49");
        assertThat(out.tvaId()).isEqualTo(8L);

        // Vérifie la persistance et la résolution de TVA
        verify(productRepository).findById(50L);
        verify(resolveService).resolveTva(8L);
        verify(productRepository).save(existing);
        verifyNoMoreInteractions(productRepository, resolveService);
    }

    @Test
    @DisplayName("setProductDetails : ne change pas la TVA si tvaId est absent (copie des autres champs uniquement)")
    void setProductDetails_sans_tvaId_ne_change_pas_relation_tva() {
        // Entité existante avec une TVA en place
        Tva current = new Tva(); current.setId(3L);
        Product existing = new Product();
        existing.setId(60L);
        existing.setName("Nom X");
        existing.setUnitPriceHT(new BigDecimal("5.00"));
        existing.setTva(current);

        when(productRepository.findById(60L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // DTO ne contenant PAS de tvaId : seule la copie des champs non nuls doit s’appliquer
        ProductDto update = new ProductDto(
            60L,
            "Nom Y",
            new BigDecimal("6.00"),
            null // pas de changement de TVA
        );

        // Act
        ProductDto out = service.setProductDetails(update);

        // Assert : nom/prix mis à jour, TVA inchangée
        assertThat(out.id()).isEqualTo(60L);
        assertThat(out.name()).isEqualTo("Nom Y");
        assertThat(out.unitPriceHT()).isEqualByComparingTo("6.00");
        assertThat(out.tvaId()).isEqualTo(3L);

        verify(productRepository).findById(60L);
        verify(productRepository).save(existing);
        verifyNoInteractions(resolveService); // aucune résolution TVA requise
    }

    @Test
    @DisplayName("setProductDetails : lève EntityNotFoundException si le produit n'existe pas")
    void setProductDetails_throw_si_introuvable() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        ProductDto update = new ProductDto(999L, "Nouveau", new BigDecimal("1.00"), 1L);

        assertThatThrownBy(() -> service.setProductDetails(update))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("999");

        verify(productRepository).findById(999L);
        verifyNoMoreInteractions(productRepository);
        verifyNoInteractions(resolveService);
    }
}
