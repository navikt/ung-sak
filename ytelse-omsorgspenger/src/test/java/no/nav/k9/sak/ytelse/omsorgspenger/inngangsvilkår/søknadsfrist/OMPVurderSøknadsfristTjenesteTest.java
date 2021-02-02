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
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class OMPVurderSøknadsfristTjenesteTest {

    private OMPVurderSøknadsfristTjeneste tjeneste = new OMPVurderSøknadsfristTjeneste(null, true, LocalDate.of(2021, 1, 1));

    @Test
    void skal_godkjenne_9_måneder_søknadsfrist_for_covid19_utvidet_frist() {
        KravDokument søknad = new KravDokument(new JournalpostId(123L), LocalDateTime.now().withYear(2021).withMonth(1).withDayOfMonth(1), KravDokumentType.INNTEKTSMELDING);
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
        KravDokument søknad = new KravDokument(new JournalpostId(123L), LocalDateTime.now().withYear(2022).withMonth(1).withDayOfMonth(1), KravDokumentType.INNTEKTSMELDING);
        LocalDate startDato = LocalDate.now().withYear(2021).withMonth(1).withDayOfMonth(1);
        var arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        var virksomhet = Arbeidsgiver.virksomhet("000000000");
        var map = Map.of(søknad,
            List.of(new SøktPeriode<>(DatoIntervallEntitet.fraOgMedTilOgMed(startDato, startDato.plusMonths(12)), UttakArbeidType.ARBEIDSTAKER, virksomhet, arbeidsforholdRef, new OppgittFraværPeriode(startDato, startDato.plusMonths(12), UttakArbeidType.ARBEIDSTAKER, virksomhet, arbeidsforholdRef, Duration.ofHours(7)))));

        var søknadSetMap = tjeneste.vurderSøknadsfrist(map);

        assertThat(søknadSetMap).containsKey(søknad);
        assertThat(søknadSetMap.get(søknad)).hasSize(2);
        var actual = søknadSetMap.get(søknad).stream().sorted(Comparator.comparing(it -> it.getPeriode().getFomDato())).iterator();
        var next = actual.next();
        assertThat(next.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(startDato, startDato.withMonth(9).withDayOfMonth(30)));
        assertThat(next.getUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        next = actual.next();
        assertThat(next.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(startDato.withMonth(10).withDayOfMonth(1), startDato.plusMonths(12)));
        assertThat(next.getUtfall()).isEqualTo(Utfall.OPPFYLT);
    }


    @Test
    void skal_ta_hensyn_til_tidligere_godkjent() {
        KravDokument søknad = new KravDokument(new JournalpostId(123L), LocalDateTime.now().withYear(2021).withMonth(1).withDayOfMonth(1), KravDokumentType.INNTEKTSMELDING);
        LocalDate startDato = LocalDate.now().withYear(2020).withMonth(1).withDayOfMonth(1);
        var arbeidsforholdRef = InternArbeidsforholdRef.nyRef();
        var virksomhet = Arbeidsgiver.virksomhet("000000000");
        var oppgittFraværPeriode = new OppgittFraværPeriode(startDato, startDato.plusMonths(12), UttakArbeidType.ARBEIDSTAKER, virksomhet, arbeidsforholdRef, Duration.ofHours(7));
        var søktePerioder = List.of(new SøktPeriode<>(DatoIntervallEntitet.fraOgMedTilOgMed(startDato, startDato.plusMonths(12)), UttakArbeidType.ARBEIDSTAKER, virksomhet, arbeidsforholdRef, oppgittFraværPeriode));
        var map = Map.of(søknad, søktePerioder);

        var søknadSetMap = tjeneste.vurderSøknadsfrist(map);

        assertThat(søknadSetMap).hasSize(1);
        assertThat(søknadSetMap).containsKey(søknad);
        assertThat(søknadSetMap.get(søknad)).hasSize(2);
        var actual = søknadSetMap.get(søknad).stream().sorted(Comparator.comparing(it -> it.getPeriode().getFomDato())).iterator();
        var next = actual.next();
        assertThat(next.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(startDato, startDato.withMonth(3).withDayOfMonth(31)));
        assertThat(next.getUtfall()).isEqualTo(Utfall.IKKE_OPPFYLT);
        next = actual.next();
        assertThat(next.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(startDato.withMonth(4).withDayOfMonth(1), startDato.plusMonths(12)));
        assertThat(next.getUtfall()).isEqualTo(Utfall.OPPFYLT);

        KravDokument søknad2 = new KravDokument(new JournalpostId(124L), LocalDateTime.now().withYear(2022).withMonth(1).withDayOfMonth(1), KravDokumentType.INNTEKTSMELDING);

        var søktePerioder2 = List.of(new SøktPeriode<>(DatoIntervallEntitet.fraOgMedTilOgMed(startDato.plusMonths(6), startDato.plusMonths(12)), UttakArbeidType.ARBEIDSTAKER, virksomhet, arbeidsforholdRef, oppgittFraværPeriode));

        var map2 = Map.of(søknad, List.of(new SøktPeriode<>(DatoIntervallEntitet.fraOgMedTilOgMed(startDato, startDato.plusMonths(12)), UttakArbeidType.ARBEIDSTAKER, virksomhet, arbeidsforholdRef, oppgittFraværPeriode)), søknad2, søktePerioder2);

        var søknadSetMap2 = tjeneste.vurderSøknadsfrist(map2);

        assertThat(søknadSetMap2).hasSize(2);
        assertThat(søknadSetMap.get(søknad)).isEqualTo(søknadSetMap2.get(søknad));
        assertThat(søknadSetMap2).containsKey(søknad2);
        assertThat(søknadSetMap2.get(søknad2)).hasSize(1);
        actual = søknadSetMap2.get(søknad2).stream().sorted(Comparator.comparing(it -> it.getPeriode().getFomDato())).iterator();
        next = actual.next();
        assertThat(next.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(startDato.withMonth(7).withDayOfMonth(1), startDato.plusMonths(12)));
        assertThat(next.getUtfall()).isEqualTo(Utfall.OPPFYLT);
    }
}
