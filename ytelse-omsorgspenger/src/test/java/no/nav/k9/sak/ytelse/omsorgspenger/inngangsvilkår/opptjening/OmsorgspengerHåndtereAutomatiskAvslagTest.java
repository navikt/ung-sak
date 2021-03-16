package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.opptjening;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetKlassifisering;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OmsorgspengerHåndtereAutomatiskAvslagTest {

    private OmsorgspengerHåndtereAutomatiskAvslag avslag = new OmsorgspengerHåndtereAutomatiskAvslag(null);

    @Test
    void er_midlertidig_inaktiv_innenfor_28_dager() {
        LocalDate stp = LocalDate.of(2020, Month.JUNE, 27);
        LocalDate fom = LocalDate.of(2020, Month.APRIL, 1);
        LocalDate tom = stp.minusDays(20);

        DatoIntervallEntitet omsorgsDager = DatoIntervallEntitet.fraOgMedTilOgMed(stp, stp.plusDays(10));
        var aktiviteter = List.of(new OpptjeningAktivitet(fom, tom, OpptjeningAktivitetType.NÆRING, OpptjeningAktivitetKlassifisering.ANTATT_GODKJENT));

        assertThat(avslag.erMidlertidigInaktiv(omsorgsDager, aktiviteter));
    }

    @Test
    void er_midlertidig_inaktiv_på_28_dager() {
        LocalDate stp = LocalDate.of(2020, Month.JUNE, 27);
        LocalDate fom = LocalDate.of(2020, Month.APRIL, 1);
        LocalDate tom = stp.minusDays(28);

        DatoIntervallEntitet omsorgsDager = DatoIntervallEntitet.fraOgMedTilOgMed(stp, stp.plusDays(10));
        var aktiviteter = List.of(new OpptjeningAktivitet(fom, tom, OpptjeningAktivitetType.NÆRING, OpptjeningAktivitetKlassifisering.ANTATT_GODKJENT));

        assertThat(avslag.erMidlertidigInaktiv(omsorgsDager, aktiviteter));
    }

    @Test
    void er_midlertidig_inaktiv_på_1_dager() {
        LocalDate stp = LocalDate.of(2020, Month.JUNE, 27);
        LocalDate fom = LocalDate.of(2020, Month.APRIL, 1);
        LocalDate tom = stp.minusDays(1);

        DatoIntervallEntitet omsorgsDager = DatoIntervallEntitet.fraOgMedTilOgMed(stp, stp.plusDays(10));
        var aktiviteter = List.of(new OpptjeningAktivitet(fom, tom, OpptjeningAktivitetType.NÆRING, OpptjeningAktivitetKlassifisering.ANTATT_GODKJENT));

        assertThat(avslag.erMidlertidigInaktiv(omsorgsDager, aktiviteter));
    }

    @Test
    void er_ikke_midlertidig_inaktiv_etter_28_dager() {
        LocalDate stp = LocalDate.of(2020, Month.JUNE, 27);
        LocalDate fom = LocalDate.of(2020, Month.APRIL, 1);
        LocalDate tom = stp.minusDays(29);

        DatoIntervallEntitet omsorgsDager = DatoIntervallEntitet.fraOgMedTilOgMed(stp, stp.plusDays(10));
        var aktiviteter = List.of(new OpptjeningAktivitet(fom, tom, OpptjeningAktivitetType.NÆRING, OpptjeningAktivitetKlassifisering.ANTATT_GODKJENT));

        assertThat(!avslag.erMidlertidigInaktiv(omsorgsDager, aktiviteter));
    }

    @Test
    void er_ikke_midlertidig_inaktiv_på_0_dager() {
        LocalDate stp = LocalDate.of(2020, Month.JUNE, 27);
        LocalDate fom = LocalDate.of(2020, Month.APRIL, 1);
        LocalDate tom = stp.minusDays(0);

        DatoIntervallEntitet omsorgsDager = DatoIntervallEntitet.fraOgMedTilOgMed(stp, stp.plusDays(10));
        var aktiviteter = List.of(new OpptjeningAktivitet(fom, tom, OpptjeningAktivitetType.NÆRING, OpptjeningAktivitetKlassifisering.ANTATT_GODKJENT));

        assertThat(!avslag.erMidlertidigInaktiv(omsorgsDager, aktiviteter));
    }
}
