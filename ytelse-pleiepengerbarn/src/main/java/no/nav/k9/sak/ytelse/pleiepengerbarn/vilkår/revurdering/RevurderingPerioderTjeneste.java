package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering;

import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.util.LRUCache;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.KompletthetForBeregningTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.PeriodeMedÅrsak;
import no.nav.k9.sak.trigger.ProsessTriggere;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
public class RevurderingPerioderTjeneste {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(2, TimeUnit.HOURS);
    private final LRUCache<Saksnummer, List<InntektsmeldingMedPerioder>> cache = new LRUCache<>(1000, CACHE_ELEMENT_LIVE_TIME_MS);
    private MottatteDokumentRepository mottatteDokumentRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private ProsessTriggereRepository prosessTriggereRepository;
    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;

    @Inject
    public RevurderingPerioderTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                       InntektArbeidYtelseTjeneste iayTjeneste,
                                       ProsessTriggereRepository prosessTriggereRepository,
                                       KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.iayTjeneste = iayTjeneste;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
    }

    RevurderingPerioderTjeneste() {
        // CDI
    }

    public Set<DatoIntervallEntitet> utledPerioderFraProsessTriggere(BehandlingReferanse referanse) {
        var prosessTriggere = prosessTriggereRepository.hentGrunnlag(referanse.getBehandlingId());
        return prosessTriggere.map(ProsessTriggere::getTriggere)
            .orElseGet(Set::of)
            .stream()
            .map(Trigger::getPeriode)
            .collect(Collectors.toSet());
    }

    public Set<PeriodeMedÅrsak> utledPerioderFraProsessTriggereMedÅrsak(BehandlingReferanse referanse) {
        var prosessTriggere = prosessTriggereRepository.hentGrunnlag(referanse.getBehandlingId());
        return prosessTriggere.map(ProsessTriggere::getTriggere)
            .orElseGet(Set::of)
            .stream()
            .map(it -> new PeriodeMedÅrsak(it.getPeriode(), it.getÅrsak()))
            .collect(Collectors.toSet());
    }

    public NavigableSet<DatoIntervallEntitet> utledPerioderFraInntektsmeldinger(BehandlingReferanse referanse, NavigableSet<DatoIntervallEntitet> datoIntervallEntitets) {
        if (!referanse.erRevurdering()) {
            return new TreeSet<>();
        }
        var mottatteInntektsmeldinger = mottatteDokumentRepository.hentMottatteDokumentForBehandling(referanse.getFagsakId(),
            referanse.getBehandlingId(),
            List.of(Brevkode.INNTEKTSMELDING), false, DokumentStatus.GYLDIG);

        if (mottatteInntektsmeldinger.isEmpty()) {
            return new TreeSet<>();
        }

        var cacheEntries = cache.get(referanse.getSaksnummer());

        // Checke cache
        // if OK, return
        if (cacheErGood(cacheEntries, mottatteInntektsmeldinger)) {
            return datoIntervallEntitets.stream()
                .filter(it -> cacheEntries.stream()
                .map(InntektsmeldingMedPerioder::getPeriode)
                .filter(Objects::nonNull)
                .map(at -> kompletthetForBeregningTjeneste.utledRelevantPeriode(referanse, at))
                .anyMatch(at -> at.overlapper(it.getFomDato().minusDays(1), it.getTomDato().plusDays(1))))
                .collect(Collectors.toCollection(TreeSet::new));
        }

        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer(), referanse.getAktørId(), referanse.getFagsakYtelseType());

        var inntektsmeldingerMedPeriode = sakInntektsmeldinger.stream()
            .filter(im -> mottatteInntektsmeldinger.stream().anyMatch(at -> at.getJournalpostId().equals(im.getJournalpostId())))
            .map(im -> new InntektsmeldingMedPerioder(im.getJournalpostId(), im.getStartDatoPermisjon().map(dato -> DatoIntervallEntitet.fraOgMedTilOgMed(dato, dato)).orElse(null)))
            .collect(Collectors.toList());
        cache.put(referanse.getSaksnummer(), inntektsmeldingerMedPeriode);

        return datoIntervallEntitets.stream()
            .filter(it -> inntektsmeldingerMedPeriode.stream()
                .map(InntektsmeldingMedPerioder::getPeriode)
                .filter(Objects::nonNull)
                .map(at -> kompletthetForBeregningTjeneste.utledRelevantPeriode(referanse, at))
                .anyMatch(at -> at.overlapper(it.getFomDato().minusDays(1), it.getTomDato().plusDays(1))))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private boolean cacheErGood(List<InntektsmeldingMedPerioder> cacheEntries, List<MottattDokument> mottatteInntektsmeldinger) {
        if (cacheEntries == null) {
            return false;
        }
        return mottatteInntektsmeldinger.stream()
            .allMatch(it -> cacheEntries.stream().anyMatch(at -> at.getJournalpostId().equals(it.getJournalpostId())));
    }
}
