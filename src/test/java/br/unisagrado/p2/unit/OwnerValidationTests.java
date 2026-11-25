package br.unisagrado.p2.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Testes unitários do grupo P2 para as regras de validação da entidade Owner (RF-01 e RF-02).
 */
class OwnerValidationTests {

    private Validator validator;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.afterPropertiesSet();
        validator = factory;
    }

    private Owner validOwner() {
        Owner owner = new Owner();
        owner.setFirstName("Ana");
        owner.setLastName("TesteP2");
        owner.setAddress("Rua A, 100");
        owner.setCity("Bauru");
        owner.setTelephone("1432345678");
        return owner;
    }

    private Set<String> invalidFields(Owner owner) {
        return validator.validate(owner).stream()
            .map(ConstraintViolation::getPropertyPath)
            .map(Object::toString)
            .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("UNI-05a (CT-001): dono com todos os campos válidos não gera violação")
    void shouldAcceptValidOwner() {
        assertThat(invalidFields(validOwner())).isEmpty();
    }

    @ParameterizedTest(name = "UNI-04 telefone \"{0}\" válido? {1}")
    @DisplayName("UNI-04 (CT-005 a CT-008): telefone deve ter exatamente 10 dígitos numéricos")
    @CsvSource({
        "1432345678,   true",   // limite válido: 10 dígitos
        "143234567,    false",  // limite inferior - 1: 9 dígitos
        "14323456789,  false",  // limite superior + 1: 11 dígitos
        "14abc45678,   false",  // classe inválida: letras
        "14-3234-567,  false"   // classe inválida: símbolos
    })
    void shouldValidateTelephoneFormat(String telephone, boolean expectedValid) {
        Owner owner = validOwner();
        owner.setTelephone(telephone);

        assertThat(invalidFields(owner).contains("telephone")).isEqualTo(!expectedValid);
    }

    @Test
    @DisplayName("UNI-05b (CT-002/CT-004): campos obrigatórios vazios geram uma violação por campo")
    void shouldRejectEmptyMandatoryFields() {
        Owner owner = new Owner();
        owner.setFirstName("");
        owner.setLastName("");
        owner.setAddress("");
        owner.setCity("");
        owner.setTelephone("");

        assertThat(invalidFields(owner))
            .contains("firstName", "lastName", "address", "city", "telephone");
    }
}
