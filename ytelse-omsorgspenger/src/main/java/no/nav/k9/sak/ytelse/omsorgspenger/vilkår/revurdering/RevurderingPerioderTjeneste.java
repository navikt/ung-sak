package no.nav.k9.sak.ytelse.omsorgspenger.vilkår.revurdering;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
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
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
public class RevurderingPerioderTjeneste {

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(2, TimeUnit.HOURS);
    private final LRUCache<Saksnummer, List<InntektsmeldingMedPerioder>> cache = new LRUCache<>(1000, CACHE_ELEMENT_LIVE_TIME_MS);
    private MottatteDokumentRepository mottatteDokumentRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private ProsessTriggereRepository prosessTriggereRepository;
    private Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregningTjenester;

    @Inject
    public RevurderingPerioderTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                       InntektArbeidYtelseTjeneste iayTjeneste,
                                       ProsessTriggereRepository prosessTriggereRepository,
                                       @Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregningTjenester) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.iayTjeneste = iayTjeneste;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.inntektsmeldingerRelevantForBeregningTjenester = inntektsmeldingerRelevantForBeregningTjenester;
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

        var cacheEntries = cache.get(referanse.getSaksnummer());

        // Checke cache
        // if OK, return
        if (cacheErGood(cacheEntries, mottatteInntektsmeldinger)) {
            return cacheEntries.stream()
                .map(InntektsmeldingMedPerioder::getPeriode)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        }

        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer(), referanse.getAktørId(), referanse.getFagsakYtelseType());

        var relevanteNyeInntektsmeldinger = new ArrayList<InntektsmeldingMedPerioder>();

        var inntektsmeldingerRelevantForBeregning = InntektsmeldingerRelevantForBeregning.finnTjeneste(inntektsmeldingerRelevantForBeregningTjenester, referanse.getFagsakYtelseType());
        for (DatoIntervallEntitet periode : datoIntervallEntitets) {
            var relevanteInntektsmeldingerForPeriode = inntektsmeldingerRelevantForBeregning.utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, periode);
            var nyeRelevanteMottatteDokumenter = mottatteInntektsmeldinger.stream()
                .filter(im -> relevanteInntektsmeldingerForPeriode.stream().anyMatch(at -> Objects.equals(at.getJournalpostId(), im.getJournalpostId())))
                .toList();
            var nyeRelevanteInntektsmeldinger = sakInntektsmeldinger.stream()
                .filter(im -> nyeRelevanteMottatteDokumenter.stream().anyMatch(at -> Objects.equals(at.getJournalpostId(), im.getJournalpostId())))
                .map(im -> new InntektsmeldingMedPerioder(im.getJournalpostId(), periode))
                .toList();

            relevanteNyeInntektsmeldinger.addAll(nyeRelevanteInntektsmeldinger);
        }

        cache.put(referanse.getSaksnummer(), relevanteNyeInntektsmeldinger);

        return relevanteNyeInntektsmeldinger.stream()
            .map(InntektsmeldingMedPerioder::getPeriode)
            .filter(Objects::nonNull)
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
