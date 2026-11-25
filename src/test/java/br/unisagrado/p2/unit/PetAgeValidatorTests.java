package br.unisagrado.p2.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.samples.petclinic.rest.validation.PetAgeValidator;

/**
 * Testes unitários do grupo P2 para a regra de idade do pet (RF-06, risco R-04).
 * O validador é testado isoladamente, sem Spring, banco ou rede.
 */
class PetAgeValidatorTests {

    private final PetAgeValidator validator = new PetAgeValidator();

    private ConstraintValidatorContext context;

    private final List<String> messages = new ArrayList<>();

    @BeforeEach
    void setUp() {
        context = Mockito.mock(ConstraintValidatorContext.class);
        ConstraintViolationBuilder builder = Mockito.mock(ConstraintViolationBuilder.class);
        Mockito.when(context.buildConstraintViolationWithTemplate(Mockito.anyString())).thenAnswer(invocation -> {
            messages.add(invocation.getArgument(0));
            return builder;
        });
    }

    @Test
    @DisplayName("UNI-01 (CT-022): data de nascimento igual a hoje é válida")
    void shouldAcceptBirthDateToday() {
        assertThat(validator.isValid(LocalDate.now(), context)).isTrue();
        assertThat(messages).isEmpty();
    }

    @Test
    @DisplayName("UNI-02 (CT-023): data de nascimento amanhã é inválida")
    void shouldRejectBirthDateInTheFuture() {
        assertThat(validator.isValid(LocalDate.now().plusDays(1), context)).isFalse();
        assertThat(messages).containsExactly("Birth date cannot be in the future");
    }

    @Test
    @DisplayName("UNI-03a (CT-024): pet com exatamente 50 anos é válido (limite)")
    void shouldAcceptBirthDateExactlyFiftyYearsAgo() {
        assertThat(validator.isValid(LocalDate.now().minusYears(50), context)).isTrue();
        assertThat(messages).isEmpty();
    }

    @Test
    @DisplayName("UNI-03b (CT-024): pet com 50 anos e 1 dia é inválido (limite + 1)")
    void shouldRejectBirthDateOlderThanFiftyYears() {
        assertThat(validator.isValid(LocalDate.now().minusYears(50).minusDays(1), context)).isFalse();
        assertThat(messages).containsExactly("Birth date cannot be older than 50 years");
    }

    @Test
    @DisplayName("UNI-07: data nula é aceita pelo validador (obrigatoriedade é verificada por outra regra)")
    void shouldAcceptNullBirthDate() {
        assertThat(validator.isValid(null, context)).isTrue();
        assertThat(messages).isEmpty();
    }
}
