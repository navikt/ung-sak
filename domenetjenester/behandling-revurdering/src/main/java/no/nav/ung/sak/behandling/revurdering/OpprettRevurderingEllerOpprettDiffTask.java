package no.nav.ung.sak.behandling.revurdering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.task.FagsakProsessTask;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Kjører tilbakehopp til starten av prosessen. Brukes til rekjøring av saker som må gjøre alt på nytt.
 */
@ApplicationScoped
@ProsessTask(OpprettRevurderingEllerOpprettDiffTask.TASKNAME)
// gruppeSekvens = false for å kunne hoppe tilbake ved feilende fortsettBehandling task
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class OpprettRevurderingEllerOpprettDiffTask extends FagsakProsessTask {

    public static final String TASKNAME = "behandlingskontroll.opprettRevurderingEllerDiff";
    public static final String BEHANDLING_ÅRSAK = "behandlingArsak";
    public static final String PERIODER = "perioder";

    private static final Logger log = LoggerFactory.getLogger(OpprettRevurderingEllerOpprettDiffTask.class);
    public static final Set<BehandlingÅrsakType> REGISTERINNHENTING_ÅRSAKER = Stream.of(
        BehandlingÅrsakType.årsakerForInnhentingAvProgramperiode().stream(),
        BehandlingÅrsakType.årsakerForInnhentingAvPersonopplysninger().stream(),
        BehandlingÅrsakType.årsakerForInnhentingAvInntektOgYtelse().stream()
    ).flatMap(Function.identity()).collect(Collectors.toSet());
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private Instance<RevurderingTjeneste> revurderingTjenester;
    private KontrollbehandlingRuting kontrollbehandlingRuting;
    private ParallelleBehandlingerRepository parallelleBehandlingerRepository;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    OpprettRevurderingEllerOpprettDiffTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettRevurderingEllerOpprettDiffTask(FagsakRepository fagsakRepository,
                                                  BehandlingRepository behandlingRepository,
                                                  BehandlingLåsRepository behandlingLåsRepository,
                                                  ProsessTriggereRepository prosessTriggereRepository,
                                                  FagsakLåsRepository fagsakLåsRepository,
                                                  BehandlingsprosessApplikasjonTjeneste behandlingsprosessApplikasjonTjeneste,
                                                  BehandlingProsesseringTjeneste behandlingProsesseringTjeneste, ProsessTaskTjeneste prosessTaskTjeneste,
                                                  @Any Instance<RevurderingTjeneste> revurderingTjenester,
                                                  KontrollbehandlingRuting kontrollbehandlingRuting,
                                                  ParallelleBehandlingerRepository parallelleBehandlingerRepository,
                                                  FagsakProsessTaskRepository fagsakProsessTaskRepository) {
        super(fagsakLåsRepository, behandlingLåsRepository);
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.behandlingsprosessApplikasjonTjeneste = behandlingsprosessApplikasjonTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.revurderingTjenester = revurderingTjenester;
        this.kontrollbehandlingRuting = kontrollbehandlingRuting;
        this.parallelleBehandlingerRepository = parallelleBehandlingerRepository;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
    }

    /**
     * Låser behandlingene tasken kan endre eller opprette ny behandling fra, før fagsaklåsen tas: behandlingen på tasken,
     * alle ytelsesbehandlinger som ikke er avsluttet og siste vedtak. Da tas låsene i samme rekkefølge som i andre tasker
     * (behandling før fagsak). Spørringene laster ikke entiteter, så behandlingene leses først etter at låsene er tatt.
     */
    @Override
    protected List<String> identifiserBehandling(ProsessTaskData prosessTaskData) {
        var fagsakId = prosessTaskData.getFagsakId();
        var behandlingIder = new LinkedHashSet<>(super.identifiserBehandling(prosessTaskData));
        parallelleBehandlingerRepository.hentIkkeAvsluttedeYtelsesbehandlinger(fagsakId)
            .forEach(b -> behandlingIder.add(b.behandlingId().toString()));
        parallelleBehandlingerRepository.finnSisteAvsluttedeIkkeHenlagteYtelsesbehandlingId(fagsakId)
            .ifPresent(id -> behandlingIder.add(id.toString()));
        return List.copyOf(behandlingIder);
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var fagsakId = prosessTaskData.getFagsakId();
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        logContext(fagsak);

        var perioderOgÅrsaker = utledÅrsakOgPerioder(prosessTaskData);
        if (perioderOgÅrsaker.isEmpty()) {
            throw new IllegalArgumentException("Må ha minst en årsak for å opprette revurdering eller diff");
        }
        Set<BehandlingÅrsakType> årsaker = perioderOgÅrsaker.stream().map(ÅrsakOgPerioder::behandlingÅrsak).collect(Collectors.toSet());
        if (kontrollbehandlingRuting.skalTilKontrollbehandling(fagsak, årsaker)) {
            opprettEllerOppdaterKontrollbehandling(prosessTaskData, fagsak, perioderOgÅrsaker);
            return;
        }

        var behandlinger = behandlingRepository.hentÅpneBehandlingerIdForFagsakId(fagsakId, BehandlingType.getOrdinæreYtelseBehandlingTyper());
        if (behandlinger.isEmpty()) {
            var sisteVedtak = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteYtelsebehandling(fagsakId);
            if (sisteVedtak.isPresent() && skalUtsetteKjøring(prosessTaskData, sisteVedtak)) {
                log.info("Siste vedtatte behandling var under iverksettelse='{}'. Oppretter ny task med samme parametere som kjøres etter iverksetting", sisteVedtak.get());
                lagreKopi(prosessTaskData);
                return;
            }

            final RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(revurderingTjenester, fagsak.getYtelseType()).orElseThrow();
            if (sisteVedtak.isPresent() && revurderingTjeneste.kanRevurderingOpprettes(fagsak)) {
                var origBehandling = sisteVedtak.get();
                BehandlingÅrsakType behandlingÅrsakType = perioderOgÅrsaker.getFirst().behandlingÅrsak();// Velger første årsak som behandlingsårsak, burde vurdere om vi skal legge til alle
                var behandling = revurderingTjeneste.opprettAutomatiskRevurdering(origBehandling, behandlingÅrsakType);

                leggTilTriggere(perioderOgÅrsaker, behandling);
                log.info("Oppretter revurdering='{}' basert på '{}'", behandling, origBehandling);
                behandlingsprosessApplikasjonTjeneste.asynkStartBehandlingsprosess(behandling);
            } else {
                throw new IllegalStateException("Prøvde revurdering av sak, men fant ingen behandling revurdere");
            }
        } else {
            if (behandlinger.size() != 1) {
                throw new IllegalStateException("Fant flere åpne behandlinger");
            }
            oppdaterÅpenBehandling(behandlinger.getFirst(), perioderOgÅrsaker);
        }
    }

    /**
     * Legger kontrollen til åpen kontrollbehandling, eller oppretter kontrollbehandling fra siste vedtak. Er kontrollbehandlingen
     * eller siste vedtak under iverksetting, utsettes tasken til iverksettingen er ferdig. Har fagsaken ikke vedtak, opprettes
     * ingen kontrollbehandling. Etterkontrollen etter første vedtak finner da periodene som mangler kontroll.
     */
    private void opprettEllerOppdaterKontrollbehandling(ProsessTaskData prosessTaskData, Fagsak fagsak, List<ÅrsakOgPerioder> perioderOgÅrsaker) {
        var åpenKontrollbehandling = parallelleBehandlingerRepository.finnIkkeAvsluttetKontrollbehandling(fagsak.getId());
        if (åpenKontrollbehandling.isPresent()) {
            var kontrollbehandling = åpenKontrollbehandling.get();
            if (kontrollbehandling.status() == BehandlingStatus.IVERKSETTER_VEDTAK) {
                utsettTilIverksettingErFerdig(prosessTaskData, kontrollbehandling.behandlingId());
            } else {
                oppdaterÅpenBehandling(kontrollbehandling.behandlingId(), perioderOgÅrsaker);
            }
            return;
        }

        var sisteVedtak = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteYtelsebehandling(fagsak.getId());
        if (sisteVedtak.isEmpty()) {
            log.info("Fagsaken har ikke vedtak. Oppretter ikke kontrollbehandling.");
            return;
        }
        var origBehandling = sisteVedtak.get();
        if (origBehandling.erUnderIverksettelse()) {
            utsettTilIverksettingErFerdig(prosessTaskData, origBehandling.getId());
            return;
        }
        var revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(revurderingTjenester, fagsak.getYtelseType()).orElseThrow();
        var kontrollbehandling = revurderingTjeneste.opprettKontrollbehandling(origBehandling, BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, false);
        leggTilTriggere(perioderOgÅrsaker, kontrollbehandling);
        log.info("Oppretter kontrollbehandling {} basert på behandling {}", kontrollbehandling.getId(), origBehandling.getId());
        behandlingsprosessApplikasjonTjeneste.asynkStartBehandlingsprosess(kontrollbehandling);
    }

    /**
     * Tasken kopieres med nytt sekvensnummer for fagsaken. Kopien kjøres etter iverksettingstaskene, når behandlingen er avsluttet.
     * Har behandlingen ingen uferdige tasker i rekkefølge, ville kopien blitt kjørt med en gang. Da feiler tasken i stedet.
     */
    private void utsettTilIverksettingErFerdig(ProsessTaskData prosessTaskData, Long behandlingUnderIverksetting) {
        var fagsakId = prosessTaskData.getFagsakId();
        if (fagsakProsessTaskRepository.finnAlleÅpneTasksForAngittSøk(fagsakId, behandlingUnderIverksetting, null).isEmpty()) {
            throw new IllegalStateException("Behandling " + behandlingUnderIverksetting
                + " er under iverksetting, men har ingen uferdige tasker i rekkefølge. Kan ikke utsette kontroll av inntekt.");
        }
        var andreUferdigeTaskerForFagsakenIGruppen = prosessTaskTjeneste.finnUferdigForGruppe(prosessTaskData.getGruppe()).stream()
            .filter(t -> !Objects.equals(t.getId(), prosessTaskData.getId()))
            .anyMatch(t -> Objects.equals(t.getFagsakId(), fagsakId));
        if (andreUferdigeTaskerForFagsakenIGruppen) {
            throw new IllegalStateException("Fant flere uferdige tasker for fagsaken i gruppe " + prosessTaskData.getGruppe()
                + ". Kan ikke utsette kontroll av inntekt mens behandling " + behandlingUnderIverksetting + " er under iverksetting.");
        }
        log.info("Behandling {} er under iverksetting. Oppretter ny task med samme parametere som kjøres etter iverksettingen", behandlingUnderIverksetting);
        lagreKopi(prosessTaskData);
    }

    private void oppdaterÅpenBehandling(Long behandlingId, List<ÅrsakOgPerioder> perioderOgÅrsaker) {
        Set<BehandlingÅrsakType> årsaker = perioderOgÅrsaker.stream().map(ÅrsakOgPerioder::behandlingÅrsak).collect(Collectors.toSet());
        log.info("Fant åpen behandling='{}', kjører diff for å flytte prosessen tilbake pga {}", behandlingId, årsaker);
        var behandlingLås = behandlingRepository.taSkriveLås(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        årsaker.forEach(it -> BehandlingÅrsak.builder(it).buildFor(behandling));
        behandlingRepository.lagre(behandling, behandlingLås);
        var skalTvingeRegisterinnhenting = årsaker.stream().anyMatch(REGISTERINNHENTING_ÅRSAKER::contains);

        behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, false, skalTvingeRegisterinnhenting);

        // Legger til sist, ønsker diffen denne gir for å sette startpunkt
        leggTilTriggere(perioderOgÅrsaker, behandling);
    }

    private void lagreKopi(ProsessTaskData prosessTaskData) {
        var kopi = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        kopi.setCallIdFraEksisterende();
        kopi.setFagsakId(prosessTaskData.getFagsakId());
        Optional.ofNullable(prosessTaskData.getBehandlingId()).ifPresent(it -> kopi.setBehandling(prosessTaskData.getFagsakId(), Long.valueOf(it)));
        Optional.ofNullable(prosessTaskData.getAktørId()).ifPresent(kopi::setAktørId);
        Optional.ofNullable(prosessTaskData.getPropertyValue(PERIODER)).ifPresent(it -> kopi.setProperty(PERIODER, it));
        Optional.ofNullable(prosessTaskData.getPropertyValue(BEHANDLING_ÅRSAK)).ifPresent(it -> kopi.setProperty(BEHANDLING_ÅRSAK, it));
        Optional.ofNullable(prosessTaskData.getPayloadAsString()).ifPresent(kopi::setPayload);
        prosessTaskTjeneste.lagre(kopi);
    }

    private void leggTilTriggere(List<ÅrsakOgPerioder> perioderOgÅrsaker, Behandling behandling) {
        Set<Trigger> triggere = perioderOgÅrsaker.stream().filter(it -> !it.perioder().isEmpty())
            .flatMap(it -> it.perioder().stream().map(p -> new Trigger(it.behandlingÅrsak(), p)))
            .collect(Collectors.toSet());

        if (!triggere.isEmpty()) {
            prosessTriggereRepository.leggTil(behandling.getId(), triggere);
        }
    }

    private boolean skalUtsetteKjøring(ProsessTaskData prosessTaskData, Optional<Behandling> sisteVedtak) {
        if (sisteVedtak.map(Behandling::erUnderIverksettelse).orElse(false)) {
            var blokkererMinstEnTask = prosessTaskTjeneste.finnAlle(ProsessTaskStatus.VETO).stream().anyMatch(it -> Objects.equals(it.getBlokkertAvProsessTaskId(), prosessTaskData.getId()));
            // Dersom denne tasken blokkerer en annen task, utsetter vi kjøring av denne tasken i håp om at original behandling blir iverksatt
            if (blokkererMinstEnTask) {
                // Utsetter kjøring
                var uferdigForGruppe = prosessTaskTjeneste.finnUferdigForGruppe(prosessTaskData.getGruppe());
                if (uferdigForGruppe.size() > 1) {
                    throw new IllegalStateException("Fant flere uferdige tasks for gruppe " + prosessTaskData.getGruppe() + ". Kunne ikke utsette kjøring av task. Og vi kan ikke revurdere behandling som er under iverksettelse.");
                }
                return true;
            }
        }
        return false;
    }

    private List<ÅrsakOgPerioder> utledÅrsakOgPerioder(ProsessTaskData prosessTaskData) {
        var perioderString = prosessTaskData.getPropertyValue(PERIODER);
        final BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.fraKode(prosessTaskData.getPropertyValue(BEHANDLING_ÅRSAK));
        if (perioderString != null && !perioderString.isEmpty()) {
            return List.of(new ÅrsakOgPerioder(behandlingÅrsakType, parseToPeriodeSet(perioderString)));
        }

        var årsakOgPerioderString = prosessTaskData.getPayloadAsString();
        if (årsakOgPerioderString != null && !årsakOgPerioderString.isEmpty()) {
            ÅrsakerOgPerioder årsakOgPerioder = JsonObjectMapper.fromJson(årsakOgPerioderString, ÅrsakerOgPerioder.class);
            return årsakOgPerioder.aarsakOgPerioder();
        }

        throw new IllegalArgumentException("Må ha enten perioder eller årsakOgPerioder satt i task data");
    }

    TreeSet<DatoIntervallEntitet> parseToPeriodeSet(String perioderString) {
        return Arrays.stream(perioderString.split("\\|")).map(Periode::new).map(DatoIntervallEntitet::fra).collect(Collectors.toCollection(TreeSet::new));
    }


}
