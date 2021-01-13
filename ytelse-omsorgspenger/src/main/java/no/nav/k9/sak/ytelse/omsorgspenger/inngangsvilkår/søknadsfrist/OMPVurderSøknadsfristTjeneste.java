package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OMPVurderSøknadsfristTjeneste implements VurderSøknadsfristTjeneste<OppgittFraværPeriode> {

    private final Map<LocalDateInterval, SøknadsfristPeriodeVurderer<OppgittFraværPeriode>> avviksVurderere;
    private final SøknadsfristPeriodeVurderer<OppgittFraværPeriode> defaultVurderer = new DefaultSøknadsfristPeriodeVurderer();
    private InntektsmeldingerPerioderTjeneste inntektsmeldingerPerioderTjeneste;
    private boolean vurderSøknadsfrist;
    private LocalDate startDatoValidering = LocalDate.of(2021, 1, 1);

    OMPVurderSøknadsfristTjeneste() {
        var utvidetVurderer = new KoronaUtvidetSøknadsfristVurderer();
        this.avviksVurderere = Map.of(utvidetVurderer.periodeSomVurderes(), utvidetVurderer);
    }

    @Inject
    public OMPVurderSøknadsfristTjeneste(InntektsmeldingerPerioderTjeneste inntektsmeldingerPerioderTjeneste,
                                         @KonfigVerdi(value = "VURDER_SOKNADSFRIST", required = false, defaultVerdi = "false") boolean vurderSøknadsfrist,
                                         @KonfigVerdi(value = "enable_søknadsfrist_fradato", defaultVerdi = "2021-01-01") LocalDate startDatoValidering) {
        this();
        this.inntektsmeldingerPerioderTjeneste = inntektsmeldingerPerioderTjeneste;
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
        var inntektsmeldinger = inntektsmeldingerPerioderTjeneste.hentUtInntektsmeldingerRelevantForBehandling(referanse);
        HashMap<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> result = new HashMap<>();
        inntektsmeldinger.forEach(it -> mapTilMøknadsperiode(result, it));
        return result;
    }

    private void mapTilMøknadsperiode(HashMap<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> result, no.nav.k9.sak.domene.iay.modell.Inntektsmelding it) {
        result.put(new KravDokument(it.getJournalpostId(), it.getInnsendingstidspunkt(), KravDokumentType.INNTEKTSMELDING),
            it.getOppgittFravær()
                .stream()
                .map(pa -> new OppgittFraværPeriode(pa.getFom(), pa.getTom(), UttakArbeidType.ARBEIDSTAKER, it.getArbeidsgiver(), it.getArbeidsforholdRef(), pa.getVarighetPerDag()))
                .map(op -> new SøktPeriode<>(op.getPeriode(), op.getAktivitetType(), op.getArbeidsgiver(), op.getArbeidsforholdRef(), op))
                .collect(Collectors.toList()));
    }

    @Override
    public Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> vurderSøknadsfrist(Map<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> søknaderMedPerioder) {

        var result = new HashMap<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>>();

        for (Map.Entry<KravDokument, List<SøktPeriode<OppgittFraværPeriode>>> entry : søknaderMedPerioder.entrySet()) {
            if (vurderSøknadsfrist && entry.getKey().getInnsendingsTidspunkt().isAfter(startDatoValidering.atStartOfDay())) {
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
                    .stream()
                    .map(this::konsistens)
                    .collect(Collectors.toList()));
            } else {
                result.put(entry.getKey(), entry.getValue()
                    .stream()
                    .map(it -> new VurdertSøktPeriode<>(it.getPeriode(), it.getType(), it.getArbeidsgiver(), it.getArbeidsforholdRef(), Utfall.OPPFYLT, it.getRaw()))
                    .collect(Collectors.toList()));
            }
        }

        return result;
    }

    private VurdertSøktPeriode<OppgittFraværPeriode> konsistens(LocalDateSegment<VurdertSøktPeriode<OppgittFraværPeriode>> segment) {
        var value = segment.getValue();
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom());
        var raw = value.getRaw();
        var fraværPeriode = new OppgittFraværPeriode(segment.getFom(), segment.getTom(), raw.getAktivitetType(), raw.getArbeidsgiver(), raw.getArbeidsforholdRef(), raw.getFraværPerDag());

        return new VurdertSøktPeriode<>(periode, value.getType(), value.getArbeidsgiver(), value.getArbeidsforholdRef(), value.getUtfall(), fraværPeriode);
    }

}
