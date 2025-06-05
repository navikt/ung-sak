package no.nav.ung.sak.behandlingslager.behandling.historikk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HistorikkinnslagLinjeBuilderTest {

    @Test
    void skal_escape_underscore() {
        var tekst = HistorikkinnslagLinjeBuilder.plainTekstLinje("Dette er en _test_ med _underscore_").tilTekst();

        assertEquals("Dette er en \\_test\\_ med \\_underscore\\_", tekst);
    }

}
