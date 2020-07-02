package no.nav.k9.sak.ytelse.frisinn.beregningsresultat;

import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ErEndringIBeregningsresultatFRISINNTest {

    private BeregningsresultatEntitet orginaltResultat = BeregningsresultatEntitet.builder().medRegelInput("").medRegelSporing("").build();
    private BeregningsresultatEntitet revurderingResultat = BeregningsresultatEntitet.builder().medRegelInput("").medRegelSporing("").build();

    @Test
    public void like_perioder_og_dagsats_gir_ikke_ugunst() {
        // Arrange
        LocalDate fom = LocalDate.of(2020,4,1);
        LocalDate tom = LocalDate.of(2020,4,30);
        UttakAktivitetPeriode uttak = lagUttakPeriode(fom, tom);
        lagOrginalPeriode(fom, tom, 100);
        lagRevurderingPeriode(fom, tom, 100);

        // Act
        boolean erUgunst = vurder(Optional.of(revurderingResultat), Optional.of(orginaltResultat), uttak);

        // Assert
        assertThat(erUgunst).isFalse();
    }

    @Test
    public void like_perioder_og_ulik_dagsats_gir_ugunst() {
        // Arrange
        LocalDate fom = LocalDate.of(2020,4,1);
        LocalDate tom = LocalDate.of(2020,4,30);
        UttakAktivitetPeriode uttak = lagUttakPeriode(fom, tom);
        lagOrginalPeriode(fom, tom, 100);
        lagRevurderingPeriode(fom, tom, 99);

        // Act
        boolean erUgunst = vurder(Optional.of(revurderingResultat), Optional.of(orginaltResultat), uttak);

        // Assert
        assertThat(erUgunst).isTrue();
    }

    @Test
    public void skal_kun_hensynta_perioder_som_overlapper_uttak() {
        // Arrange
        UttakAktivitetPeriode uttak = lagUttakPeriode(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30));

        lagOrginalPeriode(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30), 100);
        lagOrginalPeriode(LocalDate.of(2020,5,1), LocalDate.of(2020,5,30), 100);

        lagRevurderingPeriode(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30), 100);
        lagRevurderingPeriode(LocalDate.of(2020,5,1), LocalDate.of(2020,5,30), 0);

        // Act
        boolean erUgunst = vurder(Optional.of(revurderingResultat), Optional.of(orginaltResultat), uttak);

        // Assert
        assertThat(erUgunst).isFalse();
    }

    @Test
    public void skal_håndtere_at_perioder_er_splittet_uten_ugunst() {
        // Arrange
        UttakAktivitetPeriode uttak = lagUttakPeriode(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30));

        lagOrginalPeriode(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30), 100);

        lagRevurderingPeriode(LocalDate.of(2020,4,1), LocalDate.of(2020,4,16), 100);
        lagRevurderingPeriode(LocalDate.of(2020,4,17), LocalDate.of(2020,4,30), 100);

        // Act
        boolean erUgunst = vurder(Optional.of(revurderingResultat), Optional.of(orginaltResultat), uttak);

        // Assert
        assertThat(erUgunst).isFalse();
    }

    @Test
    public void skal_håndtere_at_perioder_er_splittet_med_ugunst() {
        // Arrange
        UttakAktivitetPeriode uttak = lagUttakPeriode(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30));

        lagOrginalPeriode(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30), 100);

        lagRevurderingPeriode(LocalDate.of(2020,4,1), LocalDate.of(2020,4,16), 50);
        lagRevurderingPeriode(LocalDate.of(2020,4,17), LocalDate.of(2020,4,30), 70);

        // Act
        boolean erUgunst = vurder(Optional.of(revurderingResultat), Optional.of(orginaltResultat), uttak);

        // Assert
        assertThat(erUgunst).isTrue();
    }

    @Test
    public void revurdering_uten_resultat_orginal_med_skal_gi_ugunst() {
        // Arrange
        UttakAktivitetPeriode uttak = lagUttakPeriode(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30));

        lagOrginalPeriode(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30), 100);

        // Act
        boolean erUgunst = vurder(Optional.empty(), Optional.of(orginaltResultat), uttak);

        // Assert
        assertThat(erUgunst).isTrue();
    }

    @Test
    public void revurdering_med_resultat_orginal_uten_skal_ikke_gi_ugunst() {
        // Arrange
        UttakAktivitetPeriode uttak = lagUttakPeriode(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30));

        lagRevurderingPeriode(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30), 100);

        // Act
        boolean erUgunst = vurder(Optional.of(revurderingResultat), Optional.empty(), uttak);

        // Assert
        assertThat(erUgunst).isFalse();
    }

    @Test
    public void ikke_noe_resultat_er_ikke_ugunst() {
        // Arrange
        UttakAktivitetPeriode uttak = lagUttakPeriode(LocalDate.of(2020,4,1), LocalDate.of(2020,4,30));

        // Act
        boolean erUgunst = vurder(Optional.empty(), Optional.empty(), uttak);

        // Assert
        assertThat(erUgunst).isFalse();
    }

    @Test
    public void skal_kunne_vurdere_perioder_når_revurdering_starer_før_orginal() {
        // Arrange
        UttakAktivitetPeriode uttak = lagUttakPeriode(LocalDate.of(2020,6,10), LocalDate.of(2020,6,30));

        lagOrginalPeriode(LocalDate.of(2020,6,10), LocalDate.of(2020,6,30), 1257);

        lagRevurderingPeriode(LocalDate.of(2020,6,8), LocalDate.of(2020,6,30), 1326);

        // Act
        boolean erUgunst = vurder(Optional.of(revurderingResultat), Optional.of(orginaltResultat), uttak);

        // Assert
        assertThat(erUgunst).isFalse();
    }



    private boolean vurder(Optional<BeregningsresultatEntitet> revurderingResultat, Optional<BeregningsresultatEntitet> orginaltResultat, UttakAktivitetPeriode... perioder) {
        UttakAktivitet uttak = new UttakAktivitet(Arrays.asList(perioder));
        return ErEndringIBeregningsresultatFRISINN.erUgunst(revurderingResultat, orginaltResultat, uttak);
    }

    private UttakAktivitetPeriode lagUttakPeriode(LocalDate fom, LocalDate tom) {
        return new UttakAktivitetPeriode(UttakArbeidType.FRILANSER, fom ,tom);
    }

    private void lagOrginalPeriode(LocalDate fom, LocalDate tom, int dagsats) {
        lagPeriode(fom, tom, dagsats, orginaltResultat);
    }

    private void lagRevurderingPeriode(LocalDate fom, LocalDate tom, int dagsats) {
        lagPeriode(fom, tom, dagsats, revurderingResultat);
    }

    private void lagPeriode(LocalDate fom, LocalDate tom, int dagsats, BeregningsresultatEntitet resultataggregat) {
        BeregningsresultatPeriode periode = BeregningsresultatPeriode.builder().medBeregningsresultatPeriodeFomOgTom(fom, tom).build(resultataggregat);
        BeregningsresultatAndel.builder()
            .medDagsats(dagsats)
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medStillingsprosent(BigDecimal.ZERO)
            .medUtbetalingsgrad(BigDecimal.ZERO)
            .medDagsatsFraBg(dagsats)
            .medBrukerErMottaker(true)
            .medInntektskategori(Inntektskategori.FRILANSER)
            .build(periode);
    }

}
