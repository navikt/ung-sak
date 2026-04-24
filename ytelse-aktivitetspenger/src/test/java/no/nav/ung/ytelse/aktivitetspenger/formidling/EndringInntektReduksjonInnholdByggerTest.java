package no.nav.ung.ytelse.aktivitetspenger.formidling;

import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.EndringInntektReduksjonInnholdBygger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EndringInntektReduksjonInnholdByggerTest {

    // Brevet støtter ikke å beskrive ulike reuduksjonsfaktor for inntekt og ytelse.
    // Lager en test som sjekker at faktorene er like, og utsetter implementasjonen til det er aktuelt å bruke ulike reduksjonsfaktorer på aktivitetspenger
    @Test
    void testReduksjonArbeidOgYtelseProsentErLike() {
        // Directly compare the constants
        assertThat(EndringInntektReduksjonInnholdBygger.REDUKSJON_ARBEID_PROSENT)
            .isEqualTo(EndringInntektReduksjonInnholdBygger.REDUKSJON_YTELSE_PROSENT);
    }
}
