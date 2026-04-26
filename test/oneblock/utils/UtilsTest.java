package oneblock.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UtilsTest {

    @Test
    @DisplayName("translateColorCodes: null input -> null output")
    void nullPassthrough() {
        assertThat(Utils.translateColorCodes(null)).isNull();
    }

    @Test
    @DisplayName("translateColorCodes: & codes -> section-sign codes")
    void ampersandCodes() {
        String out = Utils.translateColorCodes("&chello");
        assertThat(out).isEqualTo("\u00A7chello");
    }

    @Test
    @DisplayName("translateColorCodes: no codes -> unchanged")
    void plainText() {
        assertThat(Utils.translateColorCodes("hello world")).isEqualTo("hello world");
    }

    @Test
    @DisplayName("translateColorCodes: mixed hex + legacy codes (when hex supported)")
    void mixedHexAndLegacy() {
        if (!Utils.isHexColorSupported()) return; // sanity gate for ancient API jars
        String out = Utils.translateColorCodes("&#FF00AAfoo&lbold");
        // The hex section should be replaced with a net.md_5 ChatColor sequence
        // that DOES NOT contain the literal "&#FF00AA" any more.
        assertThat(out).doesNotContain("&#FF00AA");
        assertThat(out).contains("\u00A7lbold");
    }

    @Test
    @DisplayName("findMethod: existing / missing method detection")
    void findMethodDetects() {
        assertThat(Utils.findMethod(String.class, "length")).isTrue();
        assertThat(Utils.findMethod(String.class, "thisDoesNotExist")).isFalse();
    }
}
