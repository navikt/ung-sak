package no.nav.ung.sak.behandlingslager.vilkårsavklaring;

import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class VilkårsavklaringGrunnlagTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);
    private static final LocalDateTime VURDERT_TIDSPUNKT = LocalDateTime.of(2026, 1, 15, 10, 0);

    @Test
    void setForeslåtteAvklaringer_delerIkkeReferanserFraTidligereForeslåttSettVedNyAvklaring() {
        var grunnlag = new VilkårsavklaringGrunnlag(1L, VilkårType.BOSTEDSVILKÅR);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);

        var førsteAvklaring = lagAvklaring(periode, "Første begrunnelse");
        grunnlag.setForeslåtteAvklaringer(Set.of(førsteAvklaring));
        var førsteSett = grunnlag.getForeslåtteAvklaringer();

        var andreAvklaring = lagAvklaring(periode, "Andre begrunnelse");
        grunnlag.setForeslåtteAvklaringer(Set.of(andreAvklaring));
        var andreSett = grunnlag.getForeslåtteAvklaringer();

        assertThat(førsteSett).hasSize(1);
        assertThat(førsteSett.iterator().next().getBegrunnelse()).isEqualTo("Første begrunnelse");

        assertThat(andreSett).hasSize(1);
        assertThat(andreSett.iterator().next().getBegrunnelse()).isEqualTo("Andre begrunnelse");
    }

    @Test
    void setForeslåtteAvklaringer_skalIkkeGiEndringNarInnholdErUendret() {
        var grunnlag = new VilkårsavklaringGrunnlag(1L, VilkårType.BOSTEDSVILKÅR);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);

        var opprinnelig = lagAvklaring(periode, "begrunnelse");
        grunnlag.setForeslåtteAvklaringer(Set.of(opprinnelig));

        // Ny instans med identisk innhold skal ikke endre settet - referansen skal beholdes
        grunnlag.setForeslåtteAvklaringer(Set.of(lagAvklaring(periode, "begrunnelse")));

        assertThat(grunnlag.getForeslåtteAvklaringer()).hasSize(1);
        assertThat(grunnlag.getForeslåtteAvklaringer().iterator().next().getReferanse()).isEqualTo(opprinnelig.getReferanse());
    }

    @Test
    void ferdigstillForeslåtteAvklaringer_skalIkkeEndreHolderNarIngenForeslåtteAvklaringerFinnes() {
        var grunnlag = new VilkårsavklaringGrunnlag(1L, VilkårType.BOSTEDSVILKÅR);
        assertThat(grunnlag.getFerdigstilteAvklaringer()).isEmpty();

        grunnlag.ferdigstillForeslåtteAvklaringer();

        assertThat(grunnlag.getFerdigstilteAvklaringer()).isEmpty();
    }

    @Test
    void ferdigstillForeslåtteAvklaringer_skalFlytteForeslåttOverTilFerdigstilt() {
        var grunnlag = new VilkårsavklaringGrunnlag(1L, VilkårType.BOSTEDSVILKÅR);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        var avklaring = lagAvklaring(periode, "begrunnelse");
        grunnlag.setForeslåtteAvklaringer(Set.of(avklaring));

        grunnlag.ferdigstillForeslåtteAvklaringer();

        assertThat(grunnlag.getFerdigstilteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getReferanse)
            .containsExactly(avklaring.getReferanse());
        // Foreslåtte beholdes urørt, slik at operasjonen er idempotent og fortsatt forteller hva som ble behandlet
        assertThat(grunnlag.getForeslåtteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getReferanse)
            .containsExactly(avklaring.getReferanse());
    }

    @Test
    void nyttGrunnlagMedReferanserFra_skalGiKopiAvForeslåtteOgSammeHolderreferanse() {
        var grunnlag = new VilkårsavklaringGrunnlag(1L, VilkårType.BOSTEDSVILKÅR);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        grunnlag.setForeslåtteAvklaringer(Set.of(lagAvklaring(periode, "begrunnelse")));
        grunnlag.ferdigstillForeslåtteAvklaringer();

        var nyttGrunnlag = VilkårsavklaringGrunnlag.nyttGrunnlagMedReferanserFra(grunnlag);

        assertThat(nyttGrunnlag).isEqualTo(grunnlag);
        assertThat(nyttGrunnlag.getForeslåtteAvklaringer()).hasSize(1);
        assertThat(nyttGrunnlag.getFerdigstilteAvklaringer()).hasSize(1);
    }

    @Test
    void nyttGrunnlagForBehandlingMedReferanserFra_skalIkkeInneholdeForeslåtteAvklaringer() {
        var grunnlag = new VilkårsavklaringGrunnlag(1L, VilkårType.BOSTEDSVILKÅR);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        grunnlag.setForeslåtteAvklaringer(Set.of(lagAvklaring(periode, "begrunnelse")));
        grunnlag.ferdigstillForeslåtteAvklaringer();

        var nyttGrunnlag = VilkårsavklaringGrunnlag.nyttGrunnlagForBehandlingMedReferanserFra(2L, grunnlag);

        assertThat(nyttGrunnlag.getBehandlingId()).isEqualTo(2L);
        assertThat(nyttGrunnlag.getForeslåtteAvklaringer()).isEmpty();
        // Ferdigstilte avklaringer kopieres videre til den nye behandlingen via samme holder-referanse
        assertThat(nyttGrunnlag.getFerdigstilteAvklaringer()).hasSize(1);
    }

    @Test
    void skal_bygge_tidslinje_av_foreslåtte_avklaringer() {
        var grunnlag = new VilkårsavklaringGrunnlag(1L, VilkårType.BOSTEDSVILKÅR);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        var avklaring = lagAvklaring(periode, "begrunnelse");
        grunnlag.setForeslåtteAvklaringer(Set.of(avklaring));

        var tidslinje = VilkårAvklaringHolder.byggAvklaringTidslinje(grunnlag.getForeslåtteAvklaringer());

        assertThat(tidslinje.getMinLocalDate()).isEqualTo(FOM);
        assertThat(tidslinje.getMaxLocalDate()).isEqualTo(TOM);
        assertThat(tidslinje.stream().toList()).hasSize(1);
    }

    private static VilkårPeriodeAvklaringForeslått lagAvklaring(DatoIntervallEntitet periode, String begrunnelse) {
        return new VilkårPeriodeAvklaringForeslått(
            periode,
            "IKKE_OPPFYLT_KODE",
            begrunnelse,
            true,
            null,
            null,
            "saksbehandler2",
            VURDERT_TIDSPUNKT,
            Avklaringtype.AVSLAG
        );
    }
}
