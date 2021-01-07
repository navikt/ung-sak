package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.perioder.*;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.vedtak.konfig.KonfigVerdi;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
@FagsakYtelseTypeRef("OMP")
@BehandlingTypeRef
public class OMPVurderSøknadsfristTjeneste implements VurderSøknadsfristTjeneste<OppgittFraværPeriode> {

    private final Map<LocalDateInterval, SøknadsfristPeriodeVurderer<OppgittFraværPeriode>> avviksVurderere;
    private final SøknadsfristPeriodeVurderer<OppgittFraværPeriode> defaultVurderer = new DefaultSøknadsfristPeriodeVurderer();
    private InntektsmeldingerPerioderTjeneste inntektsmeldingerPerioderTjeneste;
    private LocalDate startDatoValidering = LocalDate.of(2021, 1, 1);

    OMPVurderSøknadsfristTjeneste() {
        var utvidetVurderer = new KoronaUtvidetSøknadsfristVurderer();
        this.avviksVurderere = Map.of(utvidetVurderer.periodeSomVurderes(), utvidetVurderer);
    }

    @Inject
    public OMPVurderSøknadsfristTjeneste(InntektsmeldingerPerioderTjeneste inntektsmeldingerPerioderTjeneste,
                                         @KonfigVerdi(value = "enable.søknadsfrist.fradato", defaultVerdi = "2021-01-01") LocalDate startDatoValidering) {
        this();
        this.inntektsmeldingerPerioderTjeneste = inntektsmeldingerPerioderTjeneste;
        this.startDatoValidering = startDatoValidering;
    }

    @Override
    public Map<Søknad, Set<VurdertSøktPeriode<OppgittFraværPeriode>>> vurderSøknadsfrist(BehandlingReferanse referanse) {
        var perioderTilVurdering = hentPerioderTilVurdering(referanse);

        return vurderSøknadsfrist(perioderTilVurdering);
    }

    @Override
    public Map<Søknad, Set<SøktPeriode<OppgittFraværPeriode>>> hentPerioderTilVurdering(BehandlingReferanse referanse) {
        var inntektsmeldinger = inntektsmeldingerPerioderTjeneste.hentUtInntektsmeldingerRelevantForBehandling(referanse);
        HashMap<Søknad, Set<SøktPeriode<OppgittFraværPeriode>>> result = new HashMap<>();
        inntektsmeldinger.forEach(it -> mapTilMøknadsperiode(result, it));
        return result;
    }

    private void mapTilMøknadsperiode(HashMap<Søknad, Set<SøktPeriode<OppgittFraværPeriode>>> result, no.nav.k9.sak.domene.iay.modell.Inntektsmelding it) {
        result.put(new Søknad(it.getJournalpostId(), it.getInnsendingstidspunkt(), SøknadType.INNTEKTSMELDING),
            it.getOppgittFravær()
                .stream()
                .map(pa -> new OppgittFraværPeriode(pa.getFom(), pa.getTom(), UttakArbeidType.ARBEIDSTAKER, it.getArbeidsgiver(), it.getArbeidsforholdRef(), pa.getVarighetPerDag()))
                .map(op -> new SøktPeriode<>(op.getPeriode(), op.getAktivitetType(), op.getArbeidsgiver(), op.getArbeidsforholdRef(), op))
                .collect(Collectors.toSet()));
    }

    @Override
    public Map<Søknad, Set<VurdertSøktPeriode<OppgittFraværPeriode>>> vurderSøknadsfrist(Map<Søknad, Set<SøktPeriode<OppgittFraværPeriode>>> søknaderMedPerioder) {

        var result = new HashMap<Søknad, Set<VurdertSøktPeriode<OppgittFraværPeriode>>>();

        for (Map.Entry<Søknad, Set<SøktPeriode<OppgittFraværPeriode>>> entry : søknaderMedPerioder.entrySet()) {
            if (entry.getKey().getInnsendingsTidspunkt().isAfter(startDatoValidering.atStartOfDay())) {
                LocalDateTimeline<SøktPeriode<OppgittFraværPeriode>> timeline = new LocalDateTimeline<>(entry.getValue().stream().map(it -> new LocalDateSegment<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), it)).collect(Collectors.toList()));
                LocalDateTimeline<VurdertSøktPeriode<OppgittFraværPeriode>> vurdertTimeline = defaultVurderer.vurderPeriode(entry.getKey(), timeline);

                List<LocalDateInterval> avviksIntervallerTilVurdering = avviksVurderere.keySet().stream().filter(it -> !timeline.intersection(it).isEmpty()).collect(Collectors.toList());
                if (avviksVurderere.keySet().stream().anyMatch(it -> !timeline.intersection(it).isEmpty())) {
                    for (LocalDateInterval localDateInterval : avviksIntervallerTilVurdering) {
                        SøknadsfristPeriodeVurderer<OppgittFraværPeriode> vurderer = avviksVurderere.get(localDateInterval);
                        LocalDateTimeline<VurdertSøktPeriode<OppgittFraværPeriode>> unntaksvurdertTimeline = vurderer.vurderPeriode(entry.getKey(), timeline);

                        vurdertTimeline = vurdertTimeline.combine(unntaksvurdertTimeline, TimelineMerger::mergeSegments, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                    }
                }
                result.put(entry.getKey(), vurdertTimeline.compress()
                    .map(TimelineMerger::konsistens)
                    .stream()
                    .map(LocalDateSegment::getValue)
                    .collect(Collectors.toSet()));
            } else {
                result.put(entry.getKey(), entry.getValue()
                    .stream()
                    .map(it -> new VurdertSøktPeriode<>(it.getPeriode(), it.getType(), it.getArbeidsgiver(), it.getArbeidsforholdRef(), Utfall.OPPFYLT, it.getRaw()))
                    .collect(Collectors.toSet()));
            }
        }

        return result;
    }

}
