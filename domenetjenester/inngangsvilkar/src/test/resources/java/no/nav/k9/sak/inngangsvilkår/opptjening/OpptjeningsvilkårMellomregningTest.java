package no.nav.k9.sak.inngangsvilkår.opptjening;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;

import org.junit.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.Aktivitet;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.MellomregningOpptjeningsvilkårData;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.Opptjeningsgrunnlag;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.Opptjeningsvilkår;

public class OpptjeningsvilkårMellomregningTest {

    @Test
    public void skal_håndtere_overlappende_perioder() {
        final Opptjeningsgrunnlag grunnlag = new Opptjeningsgrunnlag(LocalDate.now(), LocalDate.now().minusMonths(10), LocalDate.now());
        final Aktivitet aktivitet = new Aktivitet(Opptjeningsvilkår.ARBEID, "123123123", Aktivitet.ReferanseType.ORGNR);

        grunnlag.leggTil(LocalDateInterval.withPeriodAfterDate(LocalDate.now().minusMonths(8), Period.ofWeeks(6)), aktivitet);
        grunnlag.leggTil(LocalDateInterval.withPeriodAfterDate(LocalDate.now().minusMonths(7), Period.ofMonths(6)), aktivitet);
        grunnlag.leggTil(LocalDateInterval.withPeriodAfterDate(LocalDate.now().minusMonths(2), Period.ofWeeks(4)), aktivitet);

        final MellomregningOpptjeningsvilkårData mellomregning = new MellomregningOpptjeningsvilkårData(grunnlag);

        assertThat(mellomregning.getAktivitetTidslinjer(true, true)).isNotEmpty();
    }
}
