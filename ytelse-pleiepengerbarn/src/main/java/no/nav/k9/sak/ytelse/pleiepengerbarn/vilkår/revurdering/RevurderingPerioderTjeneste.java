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
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.PeriodeMedÅrsak;
import no.nav.k9.sak.trigger.ProsessTriggere;
import no.nav.k9.sak.trigger.ProsessTriggereRepository;
import no.nav.k9.sak.trigger.Trigger;

@ApplicationScoped
public class RevurderingPerioderTjeneste {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(2, TimeUnit.HOURS);
    private final LRUCache<Long, InntektsmeldingerOgPerioderCacheEntry> cache = new LRUCache<>(1000, CACHE_ELEMENT_LIVE_TIME_MS);
    private MottatteDokumentRepository mottatteDokumentRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private ProsessTriggereRepository prosessTriggereRepository;

    private RevurderingInntektsmeldingPeriodeTjeneste revurderingInntektsmeldingPeriodeTjeneste;

    @Inject
    public RevurderingPerioderTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                       InntektArbeidYtelseTjeneste iayTjeneste,
                                       ProsessTriggereRepository prosessTriggereRepository,
                                       RevurderingInntektsmeldingPeriodeTjeneste revurderingInntektsmeldingPeriodeTjeneste) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.iayTjeneste = iayTjeneste;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.revurderingInntektsmeldingPeriodeTjeneste = revurderingInntektsmeldingPeriodeTjeneste;
    }

    RevurderingPerioderTjeneste() {
        // CDI
    }

    //denne funksjonen brukes til å utlede vilkårsperioder
    public Set<DatoIntervallEntitet> utledPerioderFraProsessTriggere(BehandlingReferanse referanse) {
        var prosessTriggere = prosessTriggereRepository.hentGrunnlag(referanse.getBehandlingId());
        return prosessTriggere.map(ProsessTriggere::getTriggere)
            .orElseGet(Set::of)
            .stream()
            .filter(trigger -> BehandlingÅrsakType.medførerVilkårsperioder(trigger.getÅrsak()))
            .map(Trigger::getPeriode)
            .collect(Collectors.toSet());
    }

    //denne funksjonen brukes for å tilby data til visning
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

        var cacheEntry = cache.get(referanse.getBehandlingId());

        // Checke cache
        // if OK, return
        if (cacheErGood(cacheEntry, mottatteInntektsmeldinger)) {
            return cacheEntry.getPerioder()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        }

        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer(), referanse.getAktørId(), referanse.getFagsakYtelseType());
        var påvirketTidslinje = revurderingInntektsmeldingPeriodeTjeneste.utledTidslinjeForVurderingFraInntektsmelding(referanse, sakInntektsmeldinger, mottatteInntektsmeldinger, datoIntervallEntitets);
        var perioder = påvirketTidslinje.getLocalDateIntervals().stream().map(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFomDato(), it.getTomDato()))
            .collect(Collectors.toCollection(TreeSet::new));
        cache.put(referanse.getBehandlingId(), new InntektsmeldingerOgPerioderCacheEntry(
            mottatteInntektsmeldinger.stream().map(MottattDokument::getJournalpostId).collect(Collectors.toSet()),
            perioder
        ));
        return perioder;
    }

    private boolean cacheErGood(InntektsmeldingerOgPerioderCacheEntry cacheEntry, List<MottattDokument> mottatteInntektsmeldinger) {
        if (cacheEntry == null) {
            return false;
        }
        return mottatteInntektsmeldinger.stream()
            .allMatch(it -> cacheEntry.getJournalpostIder().contains(it.getJournalpostId()));
    }


}
