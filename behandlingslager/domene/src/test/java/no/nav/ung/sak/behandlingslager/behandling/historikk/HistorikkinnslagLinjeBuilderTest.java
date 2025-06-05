package no.nav.ung.sak.behandlingslager.behandling.historikk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HistorikkinnslagLinjeBuilderTest {

    @Test
    void skal_escape_underscore() {
        var tekst = HistorikkinnslagLinjeBuilder.plainTekstLinje("Dette er en _test_ med _underscore_").tilTekst();

        assertEquals("Dette er en \\_test\\_ med \\_underscore\\_", tekst);
    }

    @Test
    void skal_escape_dobbel_underscore() {
        var tekst = HistorikkinnslagLinjeBuilder.plainTekstLinje("Dette er en __test__ med __dobbel__ __underscore__").tilTekst();

        assertEquals("Dette er en \\_\\_test\\_\\_ med \\_\\_dobbel\\_\\_ \\_\\_underscore\\_\\_", tekst);
    }

    @Test
    void skal_escape_trippel_underscore() {
        var tekst = HistorikkinnslagLinjeBuilder.plainTekstLinje("Dette er en ___test___ med ___trippel___ ___underscore___").tilTekst();

        assertEquals("Dette er en \\_\\_\\_test\\_\\_\\_ med \\_\\_\\_trippel\\_\\_\\_ \\_\\_\\_underscore\\_\\_\\_", tekst);
    }

    @Test
    void skal_escape_escaped_underscore() {
        var tekst = HistorikkinnslagLinjeBuilder.plainTekstLinje("Dette er en \\_test\\_ med \\_escaped\\_ \\_underscore\\_").tilTekst();

        assertEquals("Dette er en \\\\_test\\\\_ med \\\\_escaped\\\\_ \\\\_underscore\\\\_", tekst);
    }



}
