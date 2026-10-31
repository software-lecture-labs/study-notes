package com.lectures.business.design.sales.domain;

import com.lectures.business.design.shared.kernel.Address;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    private static final CustomerId VINET = CustomerId.of("VINET");
    private static final Address REIMS
            = new Address("59 rue de l'Abbaye", "Reims", "51100", "France");
    private static final Address PARIS
            = new Address("12 rue des Bouchers", "Paris", "75001", "France");

    private Customer newCustomer() {
        return new Customer(VINET, "Vins et alcools Chevalier", REIMS);
    }

    @Test
    @DisplayName("a customer cannot exist without an id, a company name and an address")
    void requiredPartsAreEnforced() {
        assertThatThrownBy(() -> new Customer(null, "Vins et alcools Chevalier", REIMS))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Customer(VINET, "  ", REIMS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Customer(VINET, null, REIMS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Customer(VINET, "Vins et alcools Chevalier", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("the company name is stored without surrounding whitespace")
    void companyNameIsStripped() {
        Customer customer = new Customer(VINET, "  Vins et alcools Chevalier ", REIMS);
        assertThat(customer.companyName()).isEqualTo("Vins et alcools Chevalier");
    }

    @Test
    @DisplayName("renameTo applies the same rules as the constructor")
    void renameValidatesTheNewName() {
        Customer customer = newCustomer();
        customer.renameTo(" Chevalier Vins ");
        assertThat(customer.companyName()).isEqualTo("Chevalier Vins");

        assertThatThrownBy(() -> customer.renameTo(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(customer.companyName()).isEqualTo("Chevalier Vins");
    }

    @Test
    @DisplayName("relocateTo replaces the address as a whole and rejects null")
    void relocationReplacesTheAddress() {
        Customer customer = newCustomer();
        customer.relocateTo(PARIS);
        assertThat(customer.address()).isEqualTo(PARIS);

        assertThatThrownBy(() -> customer.relocateTo(null))
                .isInstanceOf(NullPointerException.class);
        assertThat(customer.address()).isEqualTo(PARIS);
    }

    @Test
    @DisplayName("a contact is optional: absent at first, assignable and clearable")
    void contactLifecycle() {
        Customer customer = newCustomer();
        assertThat(customer.contactName()).isEmpty();

        customer.assignContact(" Paul Henriot ");
        assertThat(customer.contactName()).contains("Paul Henriot");

        customer.clearContact();
        assertThat(customer.contactName()).isEmpty();
    }

    @Test
    @DisplayName("a blank contact is rejected; clearContact is the only way to remove one")
    void blankContactIsRejected() {
        Customer customer = newCustomer();
        customer.assignContact("Paul Henriot");

        assertThatThrownBy(() -> customer.assignContact(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> customer.assignContact(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(customer.contactName()).contains("Paul Henriot");
    }

    @Test
    @DisplayName("customers are equal by identity, not by their current state")
    void equalityIsByIdentity() {
        Customer original = newCustomer();
        Customer renamedAndMoved = new Customer(CustomerId.of("vinet"), "Another Name", PARIS);
        Customer other = new Customer(CustomerId.of("TOMSP"), "Vins et alcools Chevalier", REIMS);

        assertThat(original).isEqualTo(renamedAndMoved);
        assertThat(original).hasSameHashCodeAs(renamedAndMoved);
        assertThat(original).isNotEqualTo(other);
    }
}
