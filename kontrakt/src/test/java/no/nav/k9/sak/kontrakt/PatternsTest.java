package no.nav.k9.sak.kontrakt;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PatternsTest {

    @Test
    public void fritekstfelter_skal_tillate_paragraftegn() {
        String tekst = "Beskrivelse med paragrafreferanse §123";
        assertThat(tekst).matches(Patterns.FRITEKST);
    }

}
