package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.util.LRUCache;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.mottak.repo.MottatteDokumentRepository;
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

    @Inject
    public RevurderingPerioderTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                       InntektArbeidYtelseTjeneste iayTjeneste,
                                       ProsessTriggereRepository prosessTriggereRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.iayTjeneste = iayTjeneste;
        this.prosessTriggereRepository = prosessTriggereRepository;
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

    public Set<DatoIntervallEntitet> utledPerioderFraInntektsmeldinger(BehandlingReferanse referanse) {
        if (!referanse.erRevurdering()) {
            return Set.of();
        }
        var mottatteInntektsmeldinger = mottatteDokumentRepository.hentMottatteDokumentForBehandling(referanse.getFagsakId(),
            referanse.getBehandlingId(),
            List.of(Brevkode.INNTEKTSMELDING), false, DokumentStatus.GYLDIG);

        if (mottatteInntektsmeldinger.isEmpty()) {
            return Set.of();
        }

        var cacheEntries = cache.get(referanse.getSaksnummer());

        // Checke cache
        // if OK, return
        if (cacheErGood(cacheEntries, mottatteInntektsmeldinger)) {
            return cacheEntries.stream()
                .map(InntektsmeldingMedPerioder::getPeriode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        }

        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer(), referanse.getAktørId(), referanse.getFagsakYtelseType());

        var inntektsmeldingerMedPeriode = sakInntektsmeldinger.stream()
            .filter(im -> mottatteInntektsmeldinger.stream().anyMatch(at -> at.getJournalpostId().equals(im.getJournalpostId())))
            .map(im -> new InntektsmeldingMedPerioder(im.getJournalpostId(), im.getStartDatoPermisjon().map(dato -> DatoIntervallEntitet.fraOgMedTilOgMed(dato, dato)).orElse(null)))
            .collect(Collectors.toList());
        cache.put(referanse.getSaksnummer(), inntektsmeldingerMedPeriode);

        return inntektsmeldingerMedPeriode.stream()
            .map(InntektsmeldingMedPerioder::getPeriode)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private boolean cacheErGood(List<InntektsmeldingMedPerioder> cacheEntries, List<MottattDokument> mottatteInntektsmeldinger) {
        if (cacheEntries == null) {
            return false;
        }
        return mottatteInntektsmeldinger.stream()
            .allMatch(it -> cacheEntries.stream().anyMatch(at -> at.getJournalpostId().equals(it.getJournalpostId())));
    }
}
