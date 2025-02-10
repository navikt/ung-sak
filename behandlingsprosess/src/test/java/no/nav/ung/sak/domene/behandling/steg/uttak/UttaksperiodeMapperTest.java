package no.nav.ung.sak.domene.behandling.steg.uttak;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakAvslagResultat;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class UttaksperiodeMapperTest {

    @Test
    void skal_prioritere_søkers_dødsfall_avslagsårsak() {
        final var fom = LocalDate.now();
        final var tom = LocalDate.now();
        final var resultat = UttaksperiodeMapper.mapTilUttaksperioder(List.of(
            new LocalDateTimeline<>(fom, tom, UttakAvslagResultat.medÅrsak(UngdomsytelseUttakAvslagsårsak.SØKERS_DØDSFALL)),
            new LocalDateTimeline<>(fom, tom, UttakAvslagResultat.medÅrsak(UngdomsytelseUttakAvslagsårsak.IKKE_NOK_DAGER))));

        assertThat(resultat.size()).isEqualTo(1);
        assertThat(resultat.get(0).getAvslagsårsak()).isEqualTo(UngdomsytelseUttakAvslagsårsak.SØKERS_DØDSFALL);
    }

}
