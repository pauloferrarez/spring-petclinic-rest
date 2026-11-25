package br.unisagrado.p2.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import br.unisagrado.p2.e2e.PetClinicPages.OwnerDetailPage;
import br.unisagrado.p2.e2e.PetClinicPages.OwnerFormPage;
import br.unisagrado.p2.e2e.PetClinicPages.OwnersListPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes de sistema/interface (ponta a ponta) do grupo P2 com Selenium WebDriver.
 */
class OwnerFlowsE2eTests extends E2eTestBase {

    private static final DateTimeFormatter UI_DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Test
    @DisplayName("SIS-01 (CT-001, RF-01): cadastrar dono válido e ver seus detalhes")
    void shouldRegisterOwner() {
        OwnerDetailPage detail = new OwnersListPage(driver, FRONT_URL)
            .addOwner()
            .fill("Ana", "Selenium", "Rua A, 100", "Bauru", "1432345678")
            .submit()
            .openOwner("Ana Selenium");

        assertThat(detail.fullName()).isEqualTo("Ana Selenium");
        assertThat(detail.pageText()).contains("Rua A, 100", "Bauru", "1432345678");
    }

    @Test
    @DisplayName("SIS-02 (CT-016, RF-04): buscar donos pelo prefixo do sobrenome")
    void shouldFindOwnersByLastNamePrefix() {
        OwnersListPage list = new OwnersListPage(driver, FRONT_URL).searchByLastName("Dav");

        assertThat(list.ownerNames()).containsExactlyInAnyOrder("Betty Davis", "Harold Davis");
    }

    @Test
    @DisplayName("SIS-03 (CT-021/CT-026, RF-06/RF-07): cadastrar pet e registrar visita")
    void shouldAddPetAndVisit() {
        OwnerDetailPage detail = new OwnersListPage(driver, FRONT_URL)
            .addOwner()
            .fill("Bruno", "Visitas", "Rua B, 200", "Bauru", "1498765432")
            .submit()
            .openOwner("Bruno Visitas")
            .addPet()
            .save("Rex", "2020/05/10", "dog");

        assertThat(detail.hasText("Rex")).isTrue();

        detail = detail.addVisitToPet("Rex").save(LocalDate.now().format(UI_DATE), "Vacina anual");

        assertThat(detail.hasText("Vacina anual")).isTrue();
    }

    @Test
    @DisplayName("SIS-04 (CT-006, RF-02): telefone com 9 dígitos é recusado no formulário")
    void shouldRejectTelephoneWithNineDigits() {
        OwnerFormPage form = new OwnersListPage(driver, FRONT_URL)
            .addOwner()
            .fill("Carla", "Telefone", "Rua C, 300", "Bauru", "143234567");

        assertThat(form.isTelephoneMarkedInvalid()).isTrue();
        assertThat(form.isSubmitEnabled()).isFalse();
    }
}
