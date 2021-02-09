package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OMPVurderSøknadsfristTjeneste implements VurderSøknadsfristTjeneste<OppgittFraværPeriode> {

    private final Map<LocalDateInterval, SøknadsfristPeriodeVurderer<OppgittFraværPeriode>> avviksVurderere;
    private final SøknadsfristPeriodeVurderer<OppgittFraværPeriode> defaultVurderer = new DefaultSøknadsfristPeriodeVurderer();
    private InntektsmeldingerPerioderTjeneste inntektsmeldingerPerioderTjeneste;
    private InntektsmeldingSøktePerioderMapper inntektsmeldingMapper;
    private SøknadPerioderTjeneste søknadPerioderTjeneste;
    private boolean vurderSøknadsfrist;
    private LocalDate startDatoValidering = LocalDate.of(2021, 1, 1);

    OMPVurderSøknadsfristTjeneste() {
        var utvidetVurderer = new KoronaUtvidetSøknadsfristVurderer();
        this.avviksVurderere = Map.of(utvidetVurderer.periodeSomVurderes(), utvidetVurderer);
    }

    @Inject
    public OMPVurderSøknadsfristTjeneste(InntektsmeldingerPerioderTjeneste inntektsmeldingerPerioderTjeneste,
                                         InntektsmeldingSøktePerioderMapper inntektsmeldingMapper,
                                         SøknadPerioderTjeneste søknadPerioderTjeneste,
                                         @KonfigVerdi(value = "VURDER_SOKNADSFRIST", required = false, defaultVerdi = "false") boolean vurderSøknadsfrist,
                                         @KonfigVerdi(value = "enable_søknadsfrist_fradato", defaultVerdi = "2021-01-01") LocalDate startDatoValidering) {
        this();
        this.inntektsmeldingerPerioderTjeneste = inntektsmeldingerPerioderTjeneste;
        this.inntektsmeldingMapper = inntektsmeldingMapper;
        this.søknadPerioderTjeneste = søknadPerioderTjeneste;
        this.vurderSøknadsfrist = vurderSøknadsfrist;
        this.startDatoValidering = startDatoValidering;
    }

    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> vurderSøknadsfrist(BehandlingReferanse referanse) {
        var perioderTilVurdering = hentPerioderTilVurdering(referanse);

        return vurderSøknadsfrist(perioderTilVurdering);
    }

    @Override
    public Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> hentPerioderTilVurdering(BehandlingReferanse referanse) {
        Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søktePerioder = new HashMap<>();

        var inntektsmeldinger = inntektsmeldingerPerioderTjeneste.hentUtInntektsmeldingerRelevantForBehandling(referanse);
        søktePerioder.putAll(inntektsmeldingMapper.mapTilSøktePerioder(inntektsmeldinger));
        var søktePerioderFraSøknad = søknadPerioderTjeneste.hentSøktePerioderMedKravdokument(referanse);
        søktePerioder.putAll(søktePerioderFraSøknad);

        return søktePerioder;
    }

    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> vurderSøknadsfrist(Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søknaderMedPerioder) {

        var result = new HashMap<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>>();
        var sortedKeys = søknaderMedPerioder.keySet()
            .stream()
            .sorted(Comparator.comparing(KravDokument::getInnsendingsTidspunkt))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        sortedKeys.forEach(key -> {
                var value = søknaderMedPerioder.get(key);
                if (vurderSøknadsfrist && key.getInnsendingsTidspunkt().isAfter(startDatoValidering.atStartOfDay())) {
                    var timeline = new LocalDateTimeline<>(value.stream().map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), it)).collect(Collectors.toList()));
                    var vurdertTimeline = defaultVurderer.vurderPeriode(key, timeline);

                    List<LocalDateInterval> avviksIntervallerTilVurdering = avviksVurderere.keySet().stream().filter(it -> !timeline.intersection(it).isEmpty()).collect(Collectors.toList());
                    if (avviksVurderere.keySet().stream().anyMatch(it -> !timeline.intersection(it).isEmpty())) {
                        for (LocalDateInterval localDateInterval : avviksIntervallerTilVurdering) {
                            SøknadsfristPeriodeVurderer<OppgittFraværPeriode> vurderer = avviksVurderere.get(localDateInterval);
                            LocalDateTimeline<VurdertSøktPeriode<OppgittFraværPeriode>> unntaksvurdertTimeline = vurderer.vurderPeriode(key, timeline);

                            vurdertTimeline = vurdertTimeline.combine(unntaksvurdertTimeline, TimelineMerger::mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                        }
                    }

                    if (KravDokumentType.INNTEKTSMELDING.equals(key.getType()) && vurdertTimeline.stream().anyMatch(it -> Utfall.IKKE_OPPFYLT.equals(it.getValue().getUtfall()))) {
                        var skalEndresUtfallPå = utledAvslåttePerioderSomHarTidligereVærtInnvilget(result, key, value, vurdertTimeline);
                        vurdertTimeline = vurdertTimeline.combine(skalEndresUtfallPå, TimelineMerger::mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                    }

                    result.put(key, vurdertTimeline.compress()
                        .stream()
                        .map(this::konsistens)
                        .collect(Collectors.toList()));
                } else {
                    result.put(key, value
                        .stream()
                        .map(it -> new VurdertSøktPeriode<>(it.getPeriode(), it.getType(), it.getArbeidsgiver(), it.getArbeidsforholdRef(), Utfall.OPPFYLT, it.getRaw()))
                        .collect(Collectors.toList()));
                }
            });

        return result;
    }

    // TODO: Må skrives om dette skal støtte andre dokumenter enn inntektsmeldingen
    private LocalDateTimeline<VurdertSøktPeriode<OppgittFraværPeriode>> utledAvslåttePerioderSomHarTidligereVærtInnvilget(HashMap<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> result,
                                                                                                                          KravDokument key,
                                                                                                                          List<SøktPeriode<OppgittFraværPeriode>> value,
                                                                                                                          LocalDateTimeline<VurdertSøktPeriode<OppgittFraværPeriode>> vurdertTimeline) {
        var arbeidsgiver = value.stream()
            .map(SøktPeriode::getArbeidsgiver)
            .findFirst()
            .orElseThrow();
        var arbeidsforhold = value.stream()
            .map(SøktPeriode::getArbeidsforholdRef)
            .findFirst()
            .orElseThrow();

        var tidligereGodkjentTimeline = hentUtTidligereGodkjent(result, key, arbeidsgiver, arbeidsforhold);

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

    private LocalDateTimeline<VurdertSøktPeriode<OppgittFraværPeriode>> hentUtTidligereGodkjent(HashMap<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> result, KravDokument key, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef arbeidsforhold) {
        var tidligereGodkjentTimeline = new LocalDateTimeline<VurdertSøktPeriode<OppgittFraværPeriode>>(List.of());
        var godkjentePerioder = result.entrySet()
            .stream()
            .filter(it -> key.getType().equals(it.getKey().getType()) &&
                it.getValue().stream().anyMatch(at -> matcherAktivitet(arbeidsgiver, arbeidsforhold, at)))
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

    private boolean matcherAktivitet(no.nav.k9.sak.typer.Arbeidsgiver arbeidsgiver, no.nav.k9.sak.typer.InternArbeidsforholdRef arbeidsforhold, VurdertSøktPeriode<OppgittFraværPeriode> at) {
        return at.getArbeidsgiver().equals(arbeidsgiver) && at.getArbeidsforholdRef().equals(arbeidsforhold);
    }

    private VurdertSøktPeriode<OppgittFraværPeriode> konsistens(LocalDateSegment<VurdertSøktPeriode<OppgittFraværPeriode>> segment) {
        var value = segment.getValue();
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom());
        var raw = value.getRaw();
        var fraværPeriode = new OppgittFraværPeriode(segment.getFom(), segment.getTom(), raw.getAktivitetType(), raw.getArbeidsgiver(), raw.getArbeidsforholdRef(), raw.getFraværPerDag());

        return new VurdertSøktPeriode<>(periode, value.getType(), value.getArbeidsgiver(), value.getArbeidsforholdRef(), value.getUtfall(), fraværPeriode);
    }

}
