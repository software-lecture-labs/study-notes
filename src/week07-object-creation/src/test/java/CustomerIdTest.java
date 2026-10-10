
import com.lectures.business.design.domain.CustomerId;
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
    @DisplayName("surrounding whitespace is stripped")
    void whitespaceIsStripped() {
        assertThat(CustomerId.of("  vinet  ").value()).isEqualTo("VINET");
    }

    @Test
    @DisplayName("anything that is not five letters is rejected")
    void invalidValuesAreRejected() {
        assertThatThrownBy(() -> CustomerId.of("VIN"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CustomerId.of("NOBODY"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CustomerId.of("VINE7"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CustomerId.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CustomerId.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("the rejection message carries the value as it was given")
    void messageKeepsTheRawValue() {
        assertThatThrownBy(() -> CustomerId.of("nobody"))
                .hasMessageContaining("nobody");
    }

    @Test
    @DisplayName("two ids with the same value are equal, whatever the casing")
    void equalByValue() {
        assertThat(CustomerId.of("vinet")).isEqualTo(CustomerId.of("VINET"));
        assertThat(CustomerId.of("VINET")).isNotEqualTo(CustomerId.of("TOMSP"));
        assertThat(CustomerId.of("vinet").hashCode())
                .isEqualTo(CustomerId.of("VINET").hashCode());
    }

    @Test
    @DisplayName("toString prints the value, so log and error messages stay readable")
    void toStringIsReadable() {
        assertThat(CustomerId.of("VINET")).hasToString("VINET");
    }

    @Test
    @DisplayName("ids are ordered by their value")
    void orderedByValue() {
        assertThat(CustomerId.of("ALFKI").compareTo(CustomerId.of("VINET"))).isNegative();
        assertThat(CustomerId.of("VINET").compareTo(CustomerId.of("TOMSP"))).isPositive();
        assertThat(CustomerId.of("VINET").compareTo(CustomerId.of("vinet"))).isZero();
    }
}
