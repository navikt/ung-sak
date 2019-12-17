package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.MapAndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AndelGraderingImpl;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;

public class MapAndelGraderingTest {
    @Test
    public void skalMappeAndelGraderingSN() {
        // Arrange
        LocalDate fom1 = LocalDate.now();
        LocalDate tom1 = fom1.plusWeeks(6);
        DatoIntervallEntitet p1 = DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1);
        LocalDate fom2 = fom1.plusMonths(3);
        LocalDate tom2 = tom1.plusMonths(3);
        DatoIntervallEntitet p2 = DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2);
        var vlAndelGradering = AndelGradering.builder()
            .medStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
            .leggTilGradering(new AndelGradering.Gradering(p1, BigDecimal.valueOf(50)))
            .leggTilGradering(new AndelGradering.Gradering(p2, BigDecimal.valueOf(25)))
            .build();

        // Act
        AndelGraderingImpl regelAndelGradering = MapAndelGradering.mapTilRegelAndelGradering(vlAndelGradering);

        // Assert
        assertThat(regelAndelGradering.getAktivitetStatus()).isEqualTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatusV2.SN);
        assertThat(regelAndelGradering.getGraderinger()).hasSize(2);
        assertThat(regelAndelGradering.getGraderinger()).anySatisfy(periode -> {
            assertThat(periode.getFom()).isEqualTo(fom1);
            assertThat(periode.getTom()).isEqualTo(tom1);
        });
        assertThat(regelAndelGradering.getGraderinger()).anySatisfy(periode -> {
            assertThat(periode.getFom()).isEqualTo(fom2);
            assertThat(periode.getTom()).isEqualTo(tom2);
        });
        assertThat(regelAndelGradering.getGyldigeRefusjonskrav()).isEmpty();
    }

    @Test
    public void skalMappeAndelGraderingFL() {
// Arrange
        LocalDate fom1 = LocalDate.now();
        LocalDate tom1 = fom1.plusWeeks(6);
        DatoIntervallEntitet p1 = DatoIntervallEntitet.fraOgMedTilOgMed(fom1, tom1);
        LocalDate fom2 = fom1.plusMonths(3);
        LocalDate tom2 = tom1.plusMonths(3);
        DatoIntervallEntitet p2 = DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2);
        var vlAndelGradering = AndelGradering.builder()
            .medStatus(AktivitetStatus.FRILANSER)
            .leggTilGradering(new AndelGradering.Gradering(p1, BigDecimal.valueOf(50)))
            .leggTilGradering(new AndelGradering.Gradering(p2, BigDecimal.valueOf(25)))
            .build();

        // Act
        AndelGraderingImpl regelAndelGradering = MapAndelGradering.mapTilRegelAndelGradering(vlAndelGradering);

        // Assert
        assertThat(regelAndelGradering.getAktivitetStatus()).isEqualTo(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatusV2.FL);
        assertThat(regelAndelGradering.getGraderinger()).hasSize(2);
        assertThat(regelAndelGradering.getGraderinger()).anySatisfy(periode -> {
            assertThat(periode.getFom()).isEqualTo(fom1);
            assertThat(periode.getTom()).isEqualTo(tom1);
        });
        assertThat(regelAndelGradering.getGraderinger()).anySatisfy(periode -> {
            assertThat(periode.getFom()).isEqualTo(fom2);
            assertThat(periode.getTom()).isEqualTo(tom2);
        });
        assertThat(regelAndelGradering.getGyldigeRefusjonskrav()).isEmpty();
    }
}
