package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import static no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.AktivitetIdentifikator.lagAktivitetIdentifikator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristResultat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.TimelineMerger;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.AktivitetIdentifikator;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

@Dependent
public class VurderSøknadsfrist {

    private final Map<LocalDateInterval, SøknadsfristPeriodeVurderer<OppgittFraværPeriode>> avviksVurderere;
    private final SøknadsfristPeriodeVurderer<OppgittFraværPeriode> søknadsfristVurderer = new DefaultSøknadsfristPeriodeVurderer();
    private LocalDate startDatoValidering;

    @Inject
    public VurderSøknadsfrist(@KonfigVerdi(value = "enable_søknadsfrist_fradato", defaultVerdi = "2021-01-01") LocalDate startDatoValidering) {
        var utvidetVurderer = new KoronaUtvidetSøknadsfristVurderer();
        this.avviksVurderere = Map.of(utvidetVurderer.periodeSomVurderes(), utvidetVurderer);
        this.startDatoValidering = startDatoValidering;
    }

    public Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> vurderSøknadsfrist(Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> kravdokumentMedPerioder, Optional<AvklartSøknadsfristResultat> avklartSøknadsfristResultat) {

        var result = new HashMap<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>>();
        var sortedKravdokumenterMedPerioder = kravdokumentMedPerioder.keySet()
            .stream()
            .sorted(Comparator.comparing(KravDokument::getInnsendingsTidspunkt))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        for (var dok : sortedKravdokumenterMedPerioder) {
            for (var søktPeriode : kravdokumentMedPerioder.get(dok)) {
                var aktivitetIdent = lagAktivitetIdentifikator(søktPeriode);

                List<VurdertSøktPeriode<OppgittFraværPeriode>> vurderteSøktePerioder = new ArrayList<>();
                if (dok.getInnsendingsTidspunkt().isAfter(startDatoValidering.atStartOfDay())) {
                    var avklartKravDokument = avklartSøknadsfristResultat.flatMap(it -> it.finnAvklaring(dok.getJournalpostId()));
                    var søktTimeline = new LocalDateTimeline<>(new LocalDateInterval(søktPeriode.getPeriode().getFomDato(), søktPeriode.getPeriode().getTomDato()), søktPeriode);
                    var vurdertTimeline = søknadsfristVurderer.vurderPeriode(dok, søktTimeline, avklartKravDokument);

                    // Koronaunntak søknadsfrist
                    List<LocalDateInterval> avviksIntervallerTilVurdering = avviksVurderere.keySet().stream().filter(it -> !søktTimeline.intersection(it).isEmpty()).collect(Collectors.toList());
                    if (avviksVurderere.keySet().stream().anyMatch(it -> !søktTimeline.intersection(it).isEmpty())) {
                        for (LocalDateInterval localDateInterval : avviksIntervallerTilVurdering) {
                            SøknadsfristPeriodeVurderer<OppgittFraværPeriode> vurderer = avviksVurderere.get(localDateInterval);
                            LocalDateTimeline<VurdertSøktPeriode<OppgittFraværPeriode>> unntaksvurdertTimeline = vurderer.vurderPeriode(dok, søktTimeline, avklartKravDokument);

                            vurdertTimeline = vurdertTimeline.combine(unntaksvurdertTimeline, TimelineMerger::mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                        }
                    }

                    // Sjekk mot tidligere oppfylt dersom ikke oppfylt
                    if (vurdertTimeline.stream().anyMatch(it -> Utfall.IKKE_OPPFYLT.equals(it.getValue().getUtfall()))) {
                        var timelineSkalEndresUtfallPå = utledAvslåttePerioderSomHarTidligereVærtInnvilget(result, dok, vurdertTimeline, aktivitetIdent);
                        vurdertTimeline = vurdertTimeline.combine(timelineSkalEndresUtfallPå, TimelineMerger::mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                    }

                    LocalDateTimeline<VurdertSøktPeriode<OppgittFraværPeriode>> komprimertTimeline = VurdertSøktPeriode.compress(vurdertTimeline,
                        (p, v1, v2) -> new OppgittFraværPeriode(p.getFomDato(), p.getTomDato(), v1 /* TBD: tar bare ene verdien foreløpig */));

                    vurderteSøktePerioder.addAll(komprimertTimeline
                        .stream()
                        .map(this::konsistens)
                        .collect(Collectors.toList()));
                } else {
                    vurderteSøktePerioder.add(new VurdertSøktPeriode<>(
                        søktPeriode.getPeriode(), søktPeriode.getType(), søktPeriode.getArbeidsgiver(), søktPeriode.getArbeidsforholdRef(), Utfall.OPPFYLT, søktPeriode.getRaw()));
                }
                var gjeldende = result.getOrDefault(dok, new ArrayList<>());
                gjeldende.addAll(vurderteSøktePerioder);
                result.put(dok, gjeldende);
            }
        }

        return result;
    }

    private LocalDateTimeline<VurdertSøktPeriode<OppgittFraværPeriode>> utledAvslåttePerioderSomHarTidligereVærtInnvilget(
        Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> result,
        KravDokument kravDokument,
        LocalDateTimeline<VurdertSøktPeriode<OppgittFraværPeriode>> vurdertTimeline,
        AktivitetIdentifikator aktivitetIdent) {

        var tidligereGodkjentTimeline = hentUtTidligereGodkjent(result, kravDokument, aktivitetIdent);

        var avslått = new LocalDateTimeline<>(vurdertTimeline.toSegments()
            .stream()
            .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getValue().getUtfall()))
            .collect(Collectors.toSet()));

        var skalGodkjennes = avslått.intersection(tidligereGodkjentTimeline)
            .toSegments()
            .stream()
            .map(this::konsistens)
            .peek(it -> it.justerUtfall(Utfall.OPPFYLT))
            .map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), it))
            .collect(Collectors.toSet());

        return new LocalDateTimeline<>(skalGodkjennes);
    }

    private LocalDateTimeline<VurdertSøktPeriode<OppgittFraværPeriode>> hentUtTidligereGodkjent(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> result,
                                                                                                KravDokument kravDokument,
                                                                                                AktivitetIdentifikator aktivitetIdent) {
        var tidligereGodkjentTimeline = new LocalDateTimeline<VurdertSøktPeriode<OppgittFraværPeriode>>(List.of());
        var godkjentePerioder = result.entrySet()
            .stream()
            .filter(entry ->
                entry.getKey().getType() == kravDokument.getType() &&
                entry.getValue().stream().anyMatch(søktPeriode -> lagAktivitetIdentifikator(søktPeriode).gjelderSamme(aktivitetIdent)))
            .map(it -> it.getValue()
                .stream()
                .filter(at -> Utfall.OPPFYLT.equals(at.getUtfall()))
                .map(at -> new LocalDateSegment<>(at.getPeriode().getFomDato(), at.getPeriode().getTomDato(), at))
                .collect(Collectors.toList()))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        for (LocalDateSegment<VurdertSøktPeriode<OppgittFraværPeriode>> segment : godkjentePerioder) {
            var other = new LocalDateTimeline<>(List.of(segment));
            tidligereGodkjentTimeline = tidligereGodkjentTimeline.combine(other, TimelineMerger::mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        return tidligereGodkjentTimeline;
    }

    private VurdertSøktPeriode<OppgittFraværPeriode> konsistens(LocalDateSegment<VurdertSøktPeriode<OppgittFraværPeriode>> segment) {
        var value = segment.getValue();
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom());
        var raw = value.getRaw();
        var fraværPeriode = new OppgittFraværPeriode(raw.getJournalpostId(), segment.getFom(), segment.getTom(),
            raw.getAktivitetType(), raw.getArbeidsgiver(), raw.getArbeidsforholdRef(), raw.getFraværPerDag(),
            raw.getFraværÅrsak());

        return new VurdertSøktPeriode<>(periode, value.getType(), value.getArbeidsgiver(), value.getArbeidsforholdRef(), value.getUtfall(), fraværPeriode);
    }

}
