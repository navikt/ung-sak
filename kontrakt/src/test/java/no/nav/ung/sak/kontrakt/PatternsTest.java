package no.nav.ung.sak.kontrakt;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PatternsTest {

    @Test
    public void fritekstfelter_skal_tillate_paragraftegn() {
        String tekst = "Beskrivelse med paragrafreferanse §123";
        assertThat(tekst).matches(Patterns.FRITEKST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"«", "»", "“", "”", "‘", "’", "…", "•", "·", "–", "—"})
    public void fritekstfelter_skal_tillate_typografisk_tegnsetting_fra_tekstbehandler(String tegn) {
        String tekst = "Inntekten " + tegn + " er kontrollert";
        assertThat(tekst).matches(Patterns.FRITEKST);
        assertThat("<p>" + tekst + "</p>").matches(Patterns.FRITEKSTBREV);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\u0000", "\u0007", "\u001B", "\u200B", "\u202E", "\uFEFF"})
    public void fritekstfelter_skal_avvise_kontrolltegn_og_usynlige_formateringstegn(String tegn) {
        String tekst = "Inntekten " + tegn + " er kontrollert";
        assertThat(tekst).doesNotMatch(Patterns.FRITEKST);
        assertThat("<p>" + tekst + "</p>").doesNotMatch(Patterns.FRITEKSTBREV);
    }

}
