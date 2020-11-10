package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class ÅrskvantumTjenesteTest {

    private ÅrskvantumTjeneste tjeneste = new ÅrskvantumTjeneste();

    private Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("000000000");

    @Test
    public void skal_ta_med_alle_perioder_relevant_for_behandlingen() {
        var fravær = List.of(new OppgittFraværPeriode(LocalDate.now().minusDays(50), LocalDate.now().minusDays(48), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, null),
            new OppgittFraværPeriode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, null),
            new OppgittFraværPeriode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(4), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, Duration.ZERO),
            new OppgittFraværPeriode(LocalDate.now().minusDays(3), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, null));

        var vp = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5)),
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(3), LocalDate.now()));

        var behandlingFravær = Set.of(new OppgittFraværPeriode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(4), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, Duration.ZERO));
        var vilkårsPerioder = new TreeSet<>(vp);

        var filtrertPerioder = tjeneste.utledPerioder(vilkårsPerioder, mapTilWrappedPeriode(fravær), behandlingFravær);

        assertThat(filtrertPerioder).isNotNull();
        assertThat(filtrertPerioder).isNotEmpty();
        assertThat(filtrertPerioder).hasSize(3);
        assertThat(filtrertPerioder.stream().map(WrappedOppgittFraværPeriode::getPeriode).map(OppgittFraværPeriode::getPeriode)).containsAll(vp);
        var nullstiltPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(4), LocalDate.now().minusDays(4));
        assertThat(filtrertPerioder.stream().map(WrappedOppgittFraværPeriode::getPeriode).map(OppgittFraværPeriode::getPeriode)).contains(nullstiltPeriode);
        assertThat(filtrertPerioder.stream().map(WrappedOppgittFraværPeriode::getPeriode).map(OppgittFraværPeriode::getPeriode)).doesNotContain(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(50), LocalDate.now().minusDays(48)));
    }

    @Test
    public void skal_ta_med_alle_perioder_relevant_for_periodene_til_vurdering_og_nullstilling_utenfor() {
        var fravær = List.of(new OppgittFraværPeriode(LocalDate.now().minusDays(50), LocalDate.now().minusDays(48), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, null),
            new OppgittFraværPeriode(LocalDate.now().minusDays(47), LocalDate.now().minusDays(47), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, Duration.ZERO),
            new OppgittFraværPeriode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, null),
            new OppgittFraværPeriode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(4), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, Duration.ZERO),
            new OppgittFraværPeriode(LocalDate.now().minusDays(3), LocalDate.now(), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, null));

        var vp = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5)),
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(3), LocalDate.now()));

        var behandlingFravær = Set.of(new OppgittFraværPeriode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(4), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, Duration.ZERO),
            new OppgittFraværPeriode(LocalDate.now().minusDays(47), LocalDate.now().minusDays(47), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, Duration.ZERO));
        var vilkårsPerioder = new TreeSet<>(vp);

        var filtrertPerioder = tjeneste.utledPerioder(vilkårsPerioder, mapTilWrappedPeriode(fravær), behandlingFravær);

        assertThat(filtrertPerioder).isNotNull();
        assertThat(filtrertPerioder).isNotEmpty();
        assertThat(filtrertPerioder).hasSize(4);
        assertThat(filtrertPerioder.stream().map(WrappedOppgittFraværPeriode::getPeriode).map(OppgittFraværPeriode::getPeriode)).containsAll(vp);
        var nullstiltPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(4), LocalDate.now().minusDays(4));
        var nullstiltPeriode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(47), LocalDate.now().minusDays(47));
        assertThat(filtrertPerioder.stream().map(WrappedOppgittFraværPeriode::getPeriode).map(OppgittFraværPeriode::getPeriode)).contains(nullstiltPeriode, nullstiltPeriode1);
        assertThat(filtrertPerioder.stream().map(WrappedOppgittFraværPeriode::getPeriode).map(OppgittFraværPeriode::getPeriode)).doesNotContain(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(50), LocalDate.now().minusDays(48)));
    }

    private List<no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode> mapTilWrappedPeriode(List<OppgittFraværPeriode> perioder) {
        return perioder.stream().map(it -> new no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.WrappedOppgittFraværPeriode(it, LocalDateTime.now())).collect(Collectors.toList());
    }
}
