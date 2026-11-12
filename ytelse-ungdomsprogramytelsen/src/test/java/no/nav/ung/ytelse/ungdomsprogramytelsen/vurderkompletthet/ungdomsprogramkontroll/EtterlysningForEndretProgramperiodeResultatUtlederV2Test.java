package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll;

import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Dekker hvordan endringer i programperioden vurderes mot maksdato når det allerede finnes en ventende etterlysning.
 * Endringer som i sin helhet ligger etter maksdato håndteres av varsel om opphør ved maksdato.
 */
class EtterlysningForEndretProgramperiodeResultatUtlederV2Test {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 5);
    private static final LocalDate MAKSDATO = LocalDate.of(2026, 10, 30);

    @Test
    void skal_ikke_gi_endring_når_åpen_periode_lukkes_på_maksdato() {
        var resultat = finnResultat(TIDENES_ENDE, MAKSDATO, MAKSDATO);

        assertThat(resultat).isEqualTo(ResultatType.INGEN_ENDRING);
    }

    @Test
    void skal_ikke_gi_endring_når_åpen_periode_lukkes_etter_maksdato() {
        var resultat = finnResultat(TIDENES_ENDE, MAKSDATO.plusDays(10), MAKSDATO);

        assertThat(resultat).isEqualTo(ResultatType.INGEN_ENDRING);
    }

    @Test
    void skal_gi_endring_når_åpen_periode_lukkes_dagen_før_maksdato() {
        var resultat = finnResultat(TIDENES_ENDE, MAKSDATO.minusDays(1), MAKSDATO);

        assertThat(resultat).isEqualTo(ResultatType.ERSTATT_EKSISTERENDE_ETTERLYSNING);
    }

    @Test
    void skal_gi_endring_når_maksdato_mangler() {
        var resultat = finnResultat(TIDENES_ENDE, MAKSDATO.plusDays(10), null);

        assertThat(resultat).isEqualTo(ResultatType.ERSTATT_EKSISTERENDE_ETTERLYSNING);
    }

    private static ResultatType finnResultat(LocalDate forrigeSluttdato, LocalDate nySluttdato, LocalDate maksdato) {
        var forrigeGrunnlag = lagGrunnlag(forrigeSluttdato, maksdato);
        var gjeldendeGrunnlag = lagGrunnlag(nySluttdato, maksdato);
        var eksisterende = new EtterlysningOgGrunnlag(
            new EtterlysningStatusOgType(EtterlysningStatus.VENTER, EtterlysningType.UTTALELSE_ENDRET_PERIODE),
            forrigeGrunnlag);
        var input = new EndretUngdomsprogramEtterlysningInput(
            EtterlysningType.UTTALELSE_ENDRET_PERIODE,
            Optional.of(eksisterende),
            gjeldendeGrunnlag,
            forrigeGrunnlag,
            Optional.empty());
        return EtterlysningForEndretProgramperiodeResultatUtlederV2.finnResultat(input, null);
    }

    private static UngdomsprogramPeriodeGrunnlag lagGrunnlag(LocalDate tom, LocalDate maksdato) {
        var grunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag.getUngdomsprogramPerioder()).thenReturn(new UngdomsprogramPerioder(Set.of(new UngdomsprogramPeriode(FOM, tom))));
        when(grunnlag.getPeriodeMaksDato()).thenReturn(Optional.ofNullable(maksdato));
        return grunnlag;
    }
}
