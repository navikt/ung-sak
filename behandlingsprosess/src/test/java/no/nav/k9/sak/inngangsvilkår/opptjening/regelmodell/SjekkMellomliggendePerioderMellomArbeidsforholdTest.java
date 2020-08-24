package no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;

public class SjekkMellomliggendePerioderMellomArbeidsforholdTest {

    private SjekkMellomliggendePerioderMellomArbeidsforhold regel = new SjekkMellomliggendePerioderMellomArbeidsforhold();

    @Test
    public void skal_få_mellomliggendePeriode_mellom_arbeidsforhold_samme_arbeidsgiver_lørdagmandag() {
        var grunnlag = new Opptjeningsgrunnlag();
        var aktivitet = new Aktivitet("ARBEID", "000000000", Aktivitet.ReferanseType.ORGNR);
        grunnlag.leggTil(new LocalDateInterval(LocalDate.now().minusMonths(9), finnNærmeste(DayOfWeek.FRIDAY, LocalDate.now().minusMonths(1)).plusDays(1)), aktivitet);
        grunnlag.leggTil(new LocalDateInterval(finnNærmeste(DayOfWeek.MONDAY, LocalDate.now().minusMonths(1)), LocalDate.now()), aktivitet);

        var data = new MellomregningOpptjeningsvilkårData(grunnlag);

        regel.evaluate(data);

        var mellomliggendePerioder = data.getAkseptertMellomliggendePerioder();
        assertThat(mellomliggendePerioder).hasSize(1);
        var mellomarbeid = new Aktivitet(Opptjeningsvilkår.MELLOM_ARBEID);
        assertThat(mellomliggendePerioder).containsOnlyKeys(mellomarbeid);
        var intervaller = mellomliggendePerioder.get(mellomarbeid).getDatoIntervaller();
        assertThat(intervaller).hasSize(1);
        var interval = intervaller.first();
        assertThat(interval.getFomDato().getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(interval.getTomDato().getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
    }

    @Test
    public void skal_få_mellomliggendePeriode_mellom_arbeidsforhold_samme_arbeidsgiver() {
        var grunnlag = new Opptjeningsgrunnlag();
        var aktivitet = new Aktivitet("ARBEID", "000000000", Aktivitet.ReferanseType.ORGNR);
        grunnlag.leggTil(new LocalDateInterval(LocalDate.now().minusMonths(9), finnNærmeste(DayOfWeek.FRIDAY, LocalDate.now().minusMonths(1))), aktivitet);
        grunnlag.leggTil(new LocalDateInterval(finnNærmeste(DayOfWeek.MONDAY, LocalDate.now().minusMonths(1)), LocalDate.now()), aktivitet);

        var data = new MellomregningOpptjeningsvilkårData(grunnlag);

        regel.evaluate(data);

        var mellomliggendePerioder = data.getAkseptertMellomliggendePerioder();
        assertThat(mellomliggendePerioder).hasSize(1);
        var mellomarbeid = new Aktivitet(Opptjeningsvilkår.MELLOM_ARBEID);
        assertThat(mellomliggendePerioder).containsOnlyKeys(mellomarbeid);
        var intervaller = mellomliggendePerioder.get(mellomarbeid).getDatoIntervaller();
        assertThat(intervaller).hasSize(1);
        var interval = intervaller.first();
        assertThat(interval.getFomDato().getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
        assertThat(interval.getTomDato().getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
    }

    @Test
    public void skal_få_mellomliggendePeriode_mellom_arbeidsforhold_forskjellig_arbeidsgiver() {
        var grunnlag = new Opptjeningsgrunnlag();
        var aktivitet = new Aktivitet("ARBEID", "000000000", Aktivitet.ReferanseType.ORGNR);
        var aktivitet1 = new Aktivitet("ARBEID", "000000001", Aktivitet.ReferanseType.ORGNR);
        grunnlag.leggTil(new LocalDateInterval(LocalDate.now().minusMonths(9), finnNærmeste(DayOfWeek.FRIDAY, LocalDate.now().minusMonths(1))), aktivitet);
        grunnlag.leggTil(new LocalDateInterval(finnNærmeste(DayOfWeek.MONDAY, LocalDate.now().minusMonths(1)), LocalDate.now()), aktivitet1);

        var data = new MellomregningOpptjeningsvilkårData(grunnlag);

        regel.evaluate(data);

        var mellomliggendePerioder = data.getAkseptertMellomliggendePerioder();
        assertThat(mellomliggendePerioder).hasSize(1);
        var mellomarbeid = new Aktivitet(Opptjeningsvilkår.MELLOM_ARBEID);
        assertThat(mellomliggendePerioder).containsOnlyKeys(mellomarbeid);
        var intervaller = mellomliggendePerioder.get(mellomarbeid).getDatoIntervaller();
        assertThat(intervaller).hasSize(1);
        var interval = intervaller.first();
        assertThat(interval.getFomDato().getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
        assertThat(interval.getTomDato().getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
    }

    @Test
    public void skal_ikke_få_mellomliggendePeriode_mellom_arbeidsforhold_og_frilans() {
        var grunnlag = new Opptjeningsgrunnlag();
        var aktivitet = new Aktivitet("ARBEID", "000000000", Aktivitet.ReferanseType.ORGNR);
        var aktivitet1 = new Aktivitet("FRILANS");
        grunnlag.leggTil(new LocalDateInterval(LocalDate.now().minusMonths(9), finnNærmeste(DayOfWeek.FRIDAY, LocalDate.now().minusMonths(1))), aktivitet);
        grunnlag.leggTil(new LocalDateInterval(finnNærmeste(DayOfWeek.MONDAY, LocalDate.now().minusMonths(1)), LocalDate.now()), aktivitet1);

        var data = new MellomregningOpptjeningsvilkårData(grunnlag);

        regel.evaluate(data);

        var mellomliggendePerioder = data.getAkseptertMellomliggendePerioder();
        assertThat(mellomliggendePerioder).isEmpty();
    }

    @Test
    public void skal_ikke_få_mellomliggendePeriode_mellom_arbeidsforhold_når_ikke_på_tvers_av_helg() {
        var grunnlag = new Opptjeningsgrunnlag();
        var aktivitet = new Aktivitet("ARBEID", "000000000", Aktivitet.ReferanseType.ORGNR);
        var aktivitet1 = new Aktivitet("ARBEID", "000000001", Aktivitet.ReferanseType.ORGNR);
        grunnlag.leggTil(new LocalDateInterval(LocalDate.now().minusMonths(9), finnNærmeste(DayOfWeek.FRIDAY, LocalDate.now().minusMonths(1))), aktivitet);
        grunnlag.leggTil(new LocalDateInterval(finnNærmeste(DayOfWeek.MONDAY, LocalDate.now().minusMonths(1)).plusDays(1), LocalDate.now()), aktivitet1);

        var data = new MellomregningOpptjeningsvilkårData(grunnlag);

        regel.evaluate(data);

        var mellomliggendePerioder = data.getAkseptertMellomliggendePerioder();
        assertThat(mellomliggendePerioder).isEmpty();
    }

    private LocalDate finnNærmeste(DayOfWeek target, LocalDate date) {
        var dayOfWeek = date.getDayOfWeek();
        if (target.equals(dayOfWeek)) {
            return date;
        }
        return finnNærmeste(target, date.plusDays(1));
    }
}
