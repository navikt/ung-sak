package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.Søknad;
import no.nav.k9.sak.perioder.SøknadType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class OMPVurderSøknadsfristTjenesteTest {

    private OMPVurderSøknadsfristTjeneste tjeneste = new OMPVurderSøknadsfristTjeneste();

    @Test
    void skal_godkjenne_9_måneder_søknadsfrist_for_covid19_utvidet_frist() {
        Søknad søknad = new Søknad(new JournalpostId(123L), LocalDateTime.now().withYear(2021).withMonth(1).withDayOfMonth(1), SøknadType.INNTEKTSMELDING);
        LocalDate startDato = LocalDate.now().withYear(2020).withMonth(1).withDayOfMonth(1);
        var arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        var virksomhet = Arbeidsgiver.virksomhet("000000000");
        var oppgittFraværPeriode = new OppgittFraværPeriode(startDato, startDato.plusMonths(12), UttakArbeidType.ARBEIDSTAKER, virksomhet, arbeidsforholdRef, Duration.ofHours(7));
        var map = Map.of(søknad,
            List.of(new SøktPeriode<>(DatoIntervallEntitet.fraOgMedTilOgMed(startDato, startDato.plusMonths(12)), UttakArbeidType.ARBEIDSTAKER, virksomhet, arbeidsforholdRef, oppgittFraværPeriode)));

        var søknadSetMap = tjeneste.vurderSøknadsfrist(map);

        assertThat(søknadSetMap).containsKey(søknad);
        assertThat(søknadSetMap.get(søknad)).hasSize(2);
        var actual = søknadSetMap.get(søknad).stream().sorted(Comparator.comparing(it -> it.getPeriode().getFomDato())).iterator();
        var next = actual.next();
        assertThat(next.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(startDato, startDato.withMonth(3).withDayOfMonth(31)));
        assertThat(next.getUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        next = actual.next();
        assertThat(next.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(startDato.withMonth(4).withDayOfMonth(1), startDato.plusMonths(12)));
        assertThat(next.getUtfall()).isEqualTo(Utfall.OPPFYLT);
    }

    @Test
    void skal_vurdere_søknadsfrist() {
        Søknad søknad = new Søknad(new JournalpostId(123L), LocalDateTime.now().withYear(2022).withMonth(1).withDayOfMonth(1), SøknadType.INNTEKTSMELDING);
        LocalDate startDato = LocalDate.now().withYear(2021).withMonth(1).withDayOfMonth(1);
        var arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        var virksomhet = Arbeidsgiver.virksomhet("000000000");
        var map = Map.of(søknad,
            List.of(new SøktPeriode<>(DatoIntervallEntitet.fraOgMedTilOgMed(startDato, startDato.plusMonths(12)), UttakArbeidType.ARBEIDSTAKER, virksomhet, arbeidsforholdRef, new OppgittFraværPeriode(startDato, startDato.plusMonths(12), UttakArbeidType.ARBEIDSTAKER, virksomhet, arbeidsforholdRef, Duration.ofHours(7)))));

        var søknadSetMap = tjeneste.vurderSøknadsfrist(map);

        assertThat(søknadSetMap).containsKey(søknad);
        assertThat(søknadSetMap.get(søknad)).hasSize(2);
        var actual = søknadSetMap.get(søknad).stream().sorted(Comparator.comparing(it -> it.getPeriode().getFomDato())).iterator();
        VurdertSøktPeriode next = actual.next();
        assertThat(next.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(startDato, startDato.withMonth(9).withDayOfMonth(30)));
        assertThat(next.getUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        next = actual.next();
        assertThat(next.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(startDato.withMonth(10).withDayOfMonth(1), startDato.plusMonths(12)));
        assertThat(next.getUtfall()).isEqualTo(Utfall.OPPFYLT);
    }
}
