package br.unisagrado.p2.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;

/**
 * Testes unitários do grupo P2 para as regras de associação entre dono e pets (RF-06).
 */
class OwnerPetsTests {

    private Owner owner;

    @BeforeEach
    void setUp() {
        owner = new Owner();
        owner.setFirstName("Ana");
        owner.setLastName("TesteP2");
    }

    private Pet pet(String name, Integer id) {
        Pet pet = new Pet();
        pet.setName(name);
        pet.setId(id);
        return pet;
    }

    @Test
    @DisplayName("UNI-06a: adicionar pet associa o dono ao pet")
    void shouldLinkOwnerWhenAddingPet() {
        Pet rex = pet("Rex", 1);

        owner.addPet(rex);

        assertThat(rex.getOwner()).isSameAs(owner);
        assertThat(owner.getPets()).containsExactly(rex);
    }

    @Test
    @DisplayName("UNI-06b: pets são listados em ordem alfabética, sem diferenciar maiúsculas")
    void shouldListPetsSortedIgnoringCase() {
        owner.addPet(pet("rex", 1));
        owner.addPet(pet("Bidu", 2));
        owner.addPet(pet("mimi", 3));

        assertThat(owner.getPets()).extracting(Pet::getName).containsExactly("Bidu", "mimi", "rex");
    }

    @Test
    @DisplayName("UNI-06c: busca de pet por nome não diferencia maiúsculas de minúsculas")
    void shouldFindPetByNameIgnoringCase() {
        Pet rex = pet("Rex", 1);
        owner.addPet(rex);

        assertThat(owner.getPet("REX")).isSameAs(rex);
        assertThat(owner.getPet("Bidu")).isNull();
    }

    @Test
    @DisplayName("UNI-06d: busca ignorando pets novos não retorna pet ainda não salvo")
    void shouldIgnoreNewPetsWhenRequested() {
        Pet unsaved = pet("Rex", null);
        owner.addPet(unsaved);

        assertThat(owner.getPet("Rex", true)).isNull();
        assertThat(owner.getPet("Rex", false)).isSameAs(unsaved);
    }
}
