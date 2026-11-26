package br.unisagrado.p2.tdd;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.PetClinicApplication;
import org.springframework.samples.petclinic.rest.advice.ExceptionControllerAdvice;
import org.springframework.samples.petclinic.rest.controller.v2.OwnerRestControllerV2;
import org.springframework.samples.petclinic.rest.controller.v2.PetRestControllerV2;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.samples.petclinic.service.clinicService.ApplicationTestConfig;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * TDD do grupo P2 para o DEF-02: parâmetros de paginação fora dos limites devem ser recusados com 400
 * (e não 500), informando o parâmetro inválido. Limites da especificação: {@code page >= 0} e
 * {@code 1 <= size <= 100}. Casos CT-030 e CT-031, requisitos RF-09 e RNF-03.
 */
@SpringBootTest(classes = PetClinicApplication.class)
@ContextConfiguration(classes = ApplicationTestConfig.class)
@WebAppConfiguration
@WithMockUser(roles = "OWNER_ADMIN") // os endpoints v2 exigem esse perfil quando a segurança está ativa
class PaginationValidationTests {

    @Autowired
    private OwnerRestControllerV2 ownerController;

    @Autowired
    private PetRestControllerV2 petController;

    @MockitoBean
    private ClinicService clinicService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ownerController, petController)
            .setControllerAdvice(new ExceptionControllerAdvice())
            .build();
        // página vazia com a paginação solicitada (Page.empty() não tem número/tamanho de página)
        given(clinicService.findOwners(isNull(), any(Pageable.class)))
            .willAnswer(call -> new PageImpl<>(List.of(), call.<Pageable>getArgument(1), 0));
        given(clinicService.findPets(any(Pageable.class)))
            .willAnswer(call -> new PageImpl<>(List.of(), call.<Pageable>getArgument(0), 0));
    }

    @ParameterizedTest(name = "GET /api/v2/owners?page={0}&size={1} -> 400 ({2})")
    @CsvSource({
        "0, 0, size",
        "0, 101, size",
        "-1, 10, page",
    })
    @DisplayName("TDD-01 (DEF-02, CT-030c/d, CT-031): paginação de donos fora dos limites retorna 400")
    void shouldRejectOwnerPageOutOfRange(int page, int size, String invalidParameter) throws Exception {
        mockMvc.perform(get("/api/v2/owners").param("page", String.valueOf(page)).param("size", String.valueOf(size)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.schemaValidationErrors[*].field", Matchers.hasItem(invalidParameter)));
    }

    @ParameterizedTest(name = "GET /api/v2/owners?page={0}&size={1} -> 200")
    @CsvSource({
        "0, 1",
        "0, 100",
    })
    @DisplayName("TDD-02 (CT-030a/b): limites válidos da paginação de donos continuam retornando 200")
    void shouldAcceptOwnerPageLimits(int page, int size) throws Exception {
        mockMvc.perform(get("/api/v2/owners").param("page", String.valueOf(page)).param("size", String.valueOf(size)))
            .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "GET /api/v2/pets?page={0}&size={1} -> 400 ({2})")
    @CsvSource({
        "0, 0, size",
        "-1, 5, page",
    })
    @DisplayName("TDD-03 (DEF-02, mesma causa em /v2/pets): paginação de pets fora dos limites retorna 400")
    void shouldRejectPetPageOutOfRange(int page, int size, String invalidParameter) throws Exception {
        mockMvc.perform(get("/api/v2/pets").param("page", String.valueOf(page)).param("size", String.valueOf(size)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.schemaValidationErrors[*].field", Matchers.hasItem(invalidParameter)));
    }
}
