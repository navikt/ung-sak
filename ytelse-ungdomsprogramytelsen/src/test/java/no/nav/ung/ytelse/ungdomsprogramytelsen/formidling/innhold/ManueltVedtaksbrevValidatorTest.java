package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold;

import no.nav.ung.sak.formidling.innhold.ManueltVedtaksbrevValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManueltVedtaksbrevValidatorTest {

    @Test
    void skalKasteFeilHvisHtmlErNull() {
        byggOgAssertFeil(null, "Ingen tekst oppgitt");
    }

    @Test
    void skalKasteFeilHvisHtmlErBlank() {
        byggOgAssertFeil("    ", "Ingen tekst oppgitt");
    }

    @Test
    void skalKasteFeilHvisForsteElementIkkeErOverskrift() {
        byggOgAssertFeil("<p>Dette er ikke en overskrift</p>", "må ha overskift som første element");
    }

    @Test
    void skalKasteFeilHvisOverskriftErTom() {
        byggOgAssertFeil("<h1>   </h1><p>Innhold</p>", "har tom overskrift");
    }

    @Test
    void skalKasteFeilHvisHtmlInneholderPreutfyltOverskrift() {
        byggOgAssertFeil("<h1>Fyll inn overskrift...</h1><p>Innhold</p>", "preutfylt overskrift");
    }

    @Test
    void skalKasteFeilHvisHtmlInneholderPreutfyltBrodtekst() {
        String brevtekst = "<h1>Gyldig overskrift</h1><p>Fyll inn brødtekst...</p>";
        byggOgAssertFeil(brevtekst,
            "preutfylt brødtekst");
    }

    private void byggOgAssertFeil(String brevtekst, String feilmelding) {
        assertThatThrownBy(() -> ManueltVedtaksbrevValidator.valider(brevtekst))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(feilmelding);
    }

}
