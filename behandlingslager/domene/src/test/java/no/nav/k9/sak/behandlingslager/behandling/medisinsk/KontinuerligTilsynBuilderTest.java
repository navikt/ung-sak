package no.nav.k9.sak.behandlingslager.behandling.medisinsk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.Test;

import no.nav.k9.sak.behandlingslager.behandling.medisinsk.KontinuerligTilsynBuilder;
import no.nav.k9.sak.behandlingslager.behandling.medisinsk.KontinuerligTilsynPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class KontinuerligTilsynBuilderTest {

    @Test
    public void skal_bygge_opp_tidslinje() {
        final var builder = KontinuerligTilsynBuilder.builder();

        final var idag = LocalDate.now();
        builder.leggTil(new KontinuerligTilsynPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusWeeks(4), idag), "begrunnelse", 100));
        builder.leggTil(new KontinuerligTilsynPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusWeeks(2), idag), "begrunnelse2", 200));

        final var kontinuerligTilsyn = builder.build();

        assertThat(kontinuerligTilsyn).isNotNull();
        assertThat(kontinuerligTilsyn.getPerioder()).hasSize(2);
        assertThat(kontinuerligTilsyn.getPerioder().get(0).getPeriode().getFomDato()).isEqualTo(idag.minusWeeks(4));
        assertThat(kontinuerligTilsyn.getPerioder().get(0).getPeriode().getTomDato()).isEqualTo(idag);
        assertThat(kontinuerligTilsyn.getPerioder().get(0).getGrad()).isEqualTo(100);
        assertThat(kontinuerligTilsyn.getPerioder().get(0).getBegrunnelse()).isEqualTo("begrunnelse");
        assertThat(kontinuerligTilsyn.getPerioder().get(1).getPeriode().getFomDato()).isEqualTo(idag.minusWeeks(2));
        assertThat(kontinuerligTilsyn.getPerioder().get(1).getPeriode().getTomDato()).isEqualTo(idag);
        assertThat(kontinuerligTilsyn.getPerioder().get(1).getGrad()).isEqualTo(200);
        assertThat(kontinuerligTilsyn.getPerioder().get(1).getBegrunnelse()).isEqualTo("begrunnelse2");

        final var oppdatertBuilder = KontinuerligTilsynBuilder.builder(kontinuerligTilsyn);

        oppdatertBuilder.tilbakeStill(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusWeeks(3), idag));
        final var oppdatertTilsyn = oppdatertBuilder.build();

        assertThat(oppdatertTilsyn).isNotNull();
        assertThat(oppdatertTilsyn.getPerioder()).hasSize(1);
        assertThat(oppdatertTilsyn.getPerioder().get(0).getPeriode().getFomDato()).isEqualTo(idag.minusWeeks(4));
        assertThat(oppdatertTilsyn.getPerioder().get(0).getPeriode().getTomDato()).isEqualTo(idag.minusWeeks(3).minusDays(1));
    }
}
