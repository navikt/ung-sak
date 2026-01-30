package no.nav.ung.sak.behandling.revurdering;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.k9.prosesstask.api.*;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.felles.tid.JsonObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandling.prosessering.BehandlingsprosessApplikasjonTjeneste;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.task.FagsakProsessTask;
import no.nav.ung.sak.felles.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.felles.typer.Periode;

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
                                                  BehandlingProsesseringTjeneste behandlingProsesseringTjeneste, ProsessTaskTjeneste prosessTaskTjeneste) {
        super(fagsakLåsRepository, behandlingLåsRepository);
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.prosessTriggereRepository = prosessTriggereRepository;
        this.behandlingsprosessApplikasjonTjeneste = behandlingsprosessApplikasjonTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var fagsakId = prosessTaskData.getFagsakId();
        var fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        logContext(fagsak);

        var behandlinger = behandlingRepository.hentÅpneBehandlingerIdForFagsakId(fagsakId, BehandlingType.getYtelseBehandlingTyper());

        var perioderOgÅrsaker = utledÅrsakOgPerioder(prosessTaskData);
        if (perioderOgÅrsaker.isEmpty()) {
            throw new IllegalArgumentException("Må ha minst en årsak for å opprette revurdering eller diff");
        }
        if (behandlinger.isEmpty()) {
            var sisteVedtak = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteYtelsebehandling(fagsakId);
            if (sisteVedtak.isPresent() && skalUtsetteKjøring(prosessTaskData, sisteVedtak)) {
                log.info("Siste vedtatte behandling var under iverksettelse='{}'. Oppretter ny task med samme parametere som kjøres etter iverksetting", sisteVedtak.get());
                prosessTaskTjeneste.lagre(prosessTaskData);
                return;
            }

            final RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
            if (sisteVedtak.isPresent() && revurderingTjeneste.kanRevurderingOpprettes(fagsak)) {
                var origBehandling = sisteVedtak.get();
                BehandlingÅrsakType behandlingÅrsakType = perioderOgÅrsaker.getFirst().behandlingÅrsak();// Velger første årsak som behandlingsårsak, burde vurdere om vi skal legge til alle

                var behandling = revurderingTjeneste.opprettAutomatiskRevurdering(origBehandling, behandlingÅrsakType, origBehandling.getBehandlendeOrganisasjonsEnhet());

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
            var behandlingId = behandlinger.get(0);
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
