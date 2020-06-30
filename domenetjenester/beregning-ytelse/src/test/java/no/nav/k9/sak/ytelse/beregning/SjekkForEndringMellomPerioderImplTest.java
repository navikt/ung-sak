package no.nav.k9.sak.ytelse.beregning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;

public class SjekkForEndringMellomPerioderImplTest {

    private static final LocalDate IDAG = LocalDate.now();

    private SjekkForEndringMellomPerioder sjekkForEndringMellomPerioder;
    private BeregningsresultatEntitet brFørstegangsbehandling;
    private BeregningsresultatEntitet brRevurdering;
    private SjekkForIngenAndelerOgAndelerUtenDagsats sjekkForIngenAndelerOgAndelerUtenDagsats = Mockito.mock(SjekkForIngenAndelerOgAndelerUtenDagsats.class);
    private SjekkForEndringMellomAndelerOgFOM sjekkForEndringMellomAndelerOgFOM = Mockito.mock(SjekkForEndringMellomAndelerOgFOM.class);

    @Before
    public void oppsett() {
        brRevurdering = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        brFørstegangsbehandling = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        sjekkForEndringMellomPerioder = new SjekkForEndringMellomPerioder(
            sjekkForIngenAndelerOgAndelerUtenDagsats,
            sjekkForEndringMellomAndelerOgFOM);
    }

    @Test
    public void skal_kaste_exception_når_både_ny_og_gammel_periode_er_lik_null() {
        // Act
        Assert.assertThrows(IllegalStateException.class, () -> {
            sjekkForEndringMellomPerioder.sjekk(null, null);
        });
    }

    @Test
    public void ingen_endring_med_ingen_andel_eller_andel_uten_dagsats_og_gammelPeriode_og_ingen_nyPeriode() {
        // Arrange
        BeregningsresultatPeriode gammel = opprettPeriode(brFørstegangsbehandling, IDAG);
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(true);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(null, gammel);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void endring_med_andel_eller_andel_med_dagsats_og_gammelPeriode_og_ingen_nyPeriode() {
        // Arrange
        BeregningsresultatPeriode gammel = opprettPeriode(brFørstegangsbehandling, IDAG);
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(false);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(null, gammel);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void ingen_endring_med_ingen_andel_eller_andel_uten_dagsats_og_nyPeriode_og_ingen_gammelPeriode() {
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, IDAG);
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(true);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, null);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void endring_med_andel_eller_andel_med_dagsats_og_nyPeriode_og_ingen_gammelPeriode() {
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, IDAG);
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(false);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, null);
        // Assert
        assertThat(erEndring).isTrue();
    }

    @Test
    public void ingen_endring_med_ingen_andel_eller_andel_uten_dagsats_med_ny_og_gammel_periode_med_lik_fom_og_andeler() {
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, IDAG);
        BeregningsresultatPeriode gammel = opprettPeriode(brRevurdering, IDAG);
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(true);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(false);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void ingen_endring_med_andel_eller_andel_med_dagsats_med_ny_og_gammel_periode_med_lik_fom_og_andeler() {
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, IDAG);
        BeregningsresultatPeriode gammel = opprettPeriode(brRevurdering, IDAG);
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(false);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(false);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void ingen_endring_med_ingen_andel_eller_andel_uten_dagsats_med_ny_og_gammel_periode_med_ulik_fom_og_andeler() {
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, IDAG);
        BeregningsresultatPeriode gammel = opprettPeriode(brRevurdering, IDAG.plusDays(1));
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(true);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isFalse();
    }

    @Test
    public void endring_med_andel_eller_andel_med_dagsats_med_ny_og_gammel_periode_med_ulik_fom_og_andeler() {
        // Arrange
        BeregningsresultatPeriode ny = opprettPeriode(brRevurdering, IDAG);
        BeregningsresultatPeriode gammel = opprettPeriode(brRevurdering, IDAG.plusDays(1));
        when(sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(any(), any())).thenReturn(false);
        when(sjekkForEndringMellomAndelerOgFOM.sjekk(any(), any())).thenReturn(true);
        // Act
        boolean erEndring = sjekkForEndringMellomPerioder.sjekk(ny, gammel);
        // Assert
        assertThat(erEndring).isTrue();
    }

    private BeregningsresultatPeriode opprettPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate fom) {
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, IDAG.plusMonths(1))
            .build(beregningsresultat);
    }

}
