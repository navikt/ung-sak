package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.FraværÅrsak;
import no.nav.k9.kodeverk.uttak.SøknadÅrsak;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.AktivitetTypeArbeidsgiver;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.OppgittFraværHolder;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.OppgittFraværVerdi;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class ÅrskvantumTjenesteTest {

    private ÅrskvantumTjeneste tjeneste = new ÅrskvantumTjeneste();

    private Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet("000000000");
private LocalDateTime innsendingstidspunkt = LocalDateTime.now();

    @Test
    public void skal_ta_med_alle_perioder_relevant_for_behandlingen() {
        var jpDummy = new JournalpostId(123L);
        var fraværPrDag = new LinkedHashMap<LocalDateInterval, Duration>();
        fraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(50), LocalDate.now().minusDays(48)), null);
        fraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5)), null);
        fraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(4), LocalDate.now().minusDays(4)), Duration.ZERO);
        fraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(3), LocalDate.now()), null);
        var fraværsperioder = fraværRefusjonskrav(fraværPrDag);

        var vilkårsperioder = List.of(
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5)),
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(3), LocalDate.now()));

        var behandlingFravær = Set.of(
            new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(4), LocalDate.now().minusDays(4), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, Duration.ZERO, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT));
        var vilkårsPerioder = new TreeSet<>(vilkårsperioder);

        var filtrertPerioder = tjeneste.utledPerioder(vilkårsPerioder, fraværsperioder, behandlingFravær);


        var fasitFraværPrDag = new LinkedHashMap<LocalDateInterval, Duration>();
        fasitFraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5)), null);
        fasitFraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(4), LocalDate.now().minusDays(4)), Duration.ZERO);
        fasitFraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(3), LocalDate.now()), null);
        var fasit = fraværRefusjonskrav(fasitFraværPrDag);

        assertThat(filtrertPerioder).isEqualTo(fasit);
    }

    @Test
    public void skal_ta_med_alle_perioder_relevant_for_periodene_til_vurdering_og_nullstilling_utenfor() {
        var jpDummy = new JournalpostId(123L);

        var fraværPrDag = new LinkedHashMap<LocalDateInterval, Duration>();
        fraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(50), LocalDate.now().minusDays(48)), null);
        fraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(47), LocalDate.now().minusDays(47)), Duration.ZERO);
        fraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5)), null);
        fraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(4), LocalDate.now().minusDays(4)), Duration.ZERO);
        fraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(3), LocalDate.now()), null);
        var fraværsperioder = fraværRefusjonskrav(fraværPrDag);

        var vilkårsperioder = new TreeSet<>(Set.of(
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5)),
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(3), LocalDate.now())));

        var behandlingFravær = Set.of(
            new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(4), LocalDate.now().minusDays(4), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, Duration.ZERO, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT),
            new OppgittFraværPeriode(jpDummy, LocalDate.now().minusDays(47), LocalDate.now().minusDays(47), UttakArbeidType.ARBEIDSTAKER, arbeidsgiver, null, Duration.ZERO, FraværÅrsak.UDEFINERT, SøknadÅrsak.UDEFINERT));

        var filtrertPerioder = tjeneste.utledPerioder(vilkårsperioder, fraværsperioder, behandlingFravær);

        var fasitFraværPrDag = new LinkedHashMap<LocalDateInterval, Duration>();
        fasitFraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(47), LocalDate.now().minusDays(47)), Duration.ZERO);
        fasitFraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(10), LocalDate.now().minusDays(5)), null);
        fasitFraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(4), LocalDate.now().minusDays(4)), Duration.ZERO);
        fasitFraværPrDag.put(new LocalDateInterval(LocalDate.now().minusDays(3), LocalDate.now()), null);
        var fasit = fraværRefusjonskrav(fasitFraværPrDag);

        assertThat(filtrertPerioder).isEqualTo(fasit);
    }

    private Map<AktivitetTypeArbeidsgiver, LocalDateTimeline<OppgittFraværHolder>> fraværRefusjonskrav(Map<LocalDateInterval, Duration> perioderMedFravær) {
        FraværÅrsak fraværÅrsak = FraværÅrsak.UDEFINERT;
        SøknadÅrsak søknadÅrsak = SøknadÅrsak.UDEFINERT;
        Utfall søknadsfristUtfall = Utfall.OPPFYLT;
        return Map.of(
            new AktivitetTypeArbeidsgiver(UttakArbeidType.ARBEIDSTAKER, arbeidsgiver),
            new LocalDateTimeline<>(perioderMedFravær.entrySet().stream()
                .map(e -> new LocalDateSegment<>(e.getKey(), OppgittFraværHolder.fraRefusjonskrav(InternArbeidsforholdRef.nullRef(), new OppgittFraværVerdi(innsendingstidspunkt, e.getValue(), fraværÅrsak, søknadÅrsak, søknadsfristUtfall)))).toList()));
    }
}
