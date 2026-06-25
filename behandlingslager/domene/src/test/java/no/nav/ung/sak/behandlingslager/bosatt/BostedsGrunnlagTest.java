package no.nav.ung.sak.behandlingslager.bosatt;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BostedsGrunnlagTest {

    @Test
    void hentSøknadsfaktaSomTidslinje_flereSøknader_sammenhengendeOgSistHarÅpenEnde() {
        // Arrange
        var grunnlag = new BostedsGrunnlag(1L);
        var fom1 = LocalDate.of(2024, 1, 1);
        var fom2 = LocalDate.of(2024, 4, 1);
        var fom3 = LocalDate.of(2024, 7, 1);

        grunnlag.leggTilInformasjonFraSøknad(new BostedsinformasjonFraSøknad("JP1", fom1, true));
        grunnlag.leggTilInformasjonFraSøknad(new BostedsinformasjonFraSøknad("JP2", fom2, false));
        grunnlag.leggTilInformasjonFraSøknad(new BostedsinformasjonFraSøknad("JP3", fom3, true));

        // Act
        var tidslinje = grunnlag.hentSøknadsfaktaSomTidslinje();

        // Assert
        List<LocalDateSegment<BostedsinformasjonFraSøknad>> segmenter = tidslinje.toSegments().stream().toList();
        assertThat(segmenter).hasSize(3);

        assertThat(segmenter.get(0).getFom()).isEqualTo(fom1);
        assertThat(segmenter.get(0).getTom()).isEqualTo(fom2.minusDays(1));
        assertThat(segmenter.get(0).getValue().getJournalpostId()).isEqualTo("JP1");

        assertThat(segmenter.get(1).getFom()).isEqualTo(fom2);
        assertThat(segmenter.get(1).getTom()).isEqualTo(fom3.minusDays(1));
        assertThat(segmenter.get(1).getValue().getJournalpostId()).isEqualTo("JP2");

        assertThat(segmenter.get(2).getFom()).isEqualTo(fom3);
        assertThat(segmenter.get(2).getTom()).isEqualTo(LocalDateInterval.TIDENES_ENDE);
        assertThat(segmenter.get(2).getValue().getJournalpostId()).isEqualTo("JP3");
    }
}

