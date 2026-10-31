package com.lectures.business.design.sales.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Locale;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerIdTest {

    @Test
    @DisplayName("the value is normalised independently of the default locale")
    void normalisedWithRootLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(CustomerId.of("vinet").value()).isEqualTo("VINET");
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    @DisplayName("anything that is not five letters is rejected")
    void invalidValuesAreRejected() {
        assertThatThrownBy(() -> CustomerId.of("VIN")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CustomerId.of("VINE7")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CustomerId.of("  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CustomerId.of(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("two ids with the same value are equal")
    void equalByValue() {
        assertThat(CustomerId.of("vinet")).isEqualTo(CustomerId.of("VINET"));
        assertThat(CustomerId.of("VINET")).isNotEqualTo(CustomerId.of("TOMSP"));
    }

    @Test
    @DisplayName("toString prints the value, not the wrapper")
    void toStringIsReadable() {
        assertThat(CustomerId.of("VINET")).hasToString("VINET");
        assertThat(CustomerId.of("ALFKI")).isLessThan(CustomerId.of("VINET"));
    }
}
