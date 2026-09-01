package no.nav.ung.sak.behandlingslager.bosatt;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BostedsGrunnlagTest {

    @Test
    void setForeslåtteAvklaringer_delerIkkeReferanserFraTidligereForeslåttHolderVedNyAvklaring() {
        // Arrange
        var grunnlag = new BostedsGrunnlag(1L);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        var førsteAvklaring = lagBostedAvklaring(periode, "Første begrunnelse");

        grunnlag.setForeslåtteAvklaringer(Set.of(førsteAvklaring));
        var førsteHolder = grunnlag.getAvklaringer();

        var andreAvklaring = lagBostedAvklaring(periode, "Andre begrunnelse");
        grunnlag.setForeslåtteAvklaringer(Set.of(andreAvklaring));
        var andreHolder = grunnlag.getAvklaringer();

        // Assert - ny holder er en annen instans enn den forrige, og den forrige er urørt
        assertThat(andreHolder).isNotSameAs(førsteHolder);
        assertThat(førsteHolder.hentForeslåtteAvklaringer()).hasSize(1);
        assertThat(førsteHolder.hentForeslåtteAvklaringer().iterator().next().getBegrunnelse()).isEqualTo("Første begrunnelse");

        assertThat(andreHolder.hentForeslåtteAvklaringer()).hasSize(1);
        assertThat(andreHolder.hentForeslåtteAvklaringer().iterator().next().getBegrunnelse()).isEqualTo("Andre begrunnelse");
    }

    private static BostedsPeriodeAvklaringForeslått lagBostedAvklaring(DatoIntervallEntitet periode, String begrunnelse) {
        return new BostedsPeriodeAvklaringForeslått(
            periode,
            BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM,
            begrunnelse,
            true,
            null,
            null,
            BostedsavklaringKildeType.FOLKEREGISTER,
            null,
            "saksbehandler2",
            LocalDateTime.of(2024, 2, 1, 12, 0),
            Avklaringtype.AVSLAG
        );
    }

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
        List<LocalDateSegment<BostedsinformasjonFraSøknad>> segmenter = tidslinje.segmenter().stream().toList();
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

