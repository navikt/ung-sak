package no.nav.k9.sak.ytelse.omsorgspenger.vilkår;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;

import org.junit.Test;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class SøktePerioderTest {

    @Test
    public void skal_håndtere_overlapp_mellom_arbeidsgivere() {
        var søktePerioder = new SøktePerioder(null);
        var oppgittFravær = new OppgittFravær(new OppgittFraværPeriode(LocalDate.now().minusWeeks(6), LocalDate.now().minusDays(5), UttakArbeidType.ARBEIDSTAKER, Duration.ofMinutes(450)),
            new OppgittFraværPeriode(LocalDate.now().minusDays(5), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, Duration.ofMinutes(450)));

        var vilkårsPerioderFraSøktePerioder = søktePerioder.utledPeriodeFraSøknadsPerioder(oppgittFravær.getPerioder());

        assertThat(vilkårsPerioderFraSøktePerioder).hasSize(1);
        assertThat(vilkårsPerioderFraSøktePerioder.iterator().next()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusWeeks(6), LocalDate.now()));
    }
}
