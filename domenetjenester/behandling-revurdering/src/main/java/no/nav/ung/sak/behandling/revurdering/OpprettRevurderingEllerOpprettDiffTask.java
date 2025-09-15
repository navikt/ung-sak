package no.nav.ung.sak.behandling.revurdering;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import no.nav.k9.prosesstask.api.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
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
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;

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
    public static final String PERIODE_FOM = "fom";
    public static final String PERIODE_TOM = "tom";
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

        var behandlinger = behandlingRepository.hentÅpneBehandlingerIdForFagsakId(fagsakId);
        final BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.fraKode(prosessTaskData.getPropertyValue(BEHANDLING_ÅRSAK));
        var perioder = utledPerioder(prosessTaskData);
        if (behandlinger.isEmpty()) {
            var sisteVedtak = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsakId);
            if (sisteVedtak.isPresent() && skalUtsetteKjøring(prosessTaskData, sisteVedtak)) {
                log.info("Siste vedtatte behandling var under iverksettelse='{}'. Oppretter ny task med samme parametere som kjøres etter iverksetting", sisteVedtak.get());
                prosessTaskTjeneste.lagre(prosessTaskData);
                return;
            }

            final RevurderingTjeneste revurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(RevurderingTjeneste.class, fagsak.getYtelseType()).orElseThrow();
            if (sisteVedtak.isPresent() && revurderingTjeneste.kanRevurderingOpprettes(fagsak)) {
                var origBehandling = sisteVedtak.get();
                var behandling = revurderingTjeneste.opprettAutomatiskRevurdering(origBehandling, behandlingÅrsakType, origBehandling.getBehandlendeOrganisasjonsEnhet());
                if (perioder != null && !perioder.isEmpty()) {
                    prosessTriggereRepository.leggTil(behandling.getId(), perioder.stream().map(it -> new Trigger(behandlingÅrsakType, it)).collect(Collectors.toSet()));
                }
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
            log.info("Fant åpen behandling='{}', kjører diff for å flytte prosessen tilbake pga {}", behandlingId, behandlingÅrsakType);
            var behandlingLås = behandlingRepository.taSkriveLås(behandlingId);
            var behandling = behandlingRepository.hentBehandling(behandlingId);
            BehandlingÅrsak.builder(behandlingÅrsakType).buildFor(behandling);
            behandlingRepository.lagre(behandling, behandlingLås);
            var skalTvingeRegisterinnhenting = REGISTERINNHENTING_ÅRSAKER.contains(behandlingÅrsakType);

            behandlingProsesseringTjeneste.opprettTasksForGjenopptaOppdaterFortsett(behandling, false, skalTvingeRegisterinnhenting);

            // Legger til sist, ønsker diffen denne gir for å sette startpunkt
            if (perioder != null && !perioder.isEmpty()) {
                prosessTriggereRepository.leggTil(behandling.getId(), perioder.stream().map(it -> new Trigger(behandlingÅrsakType, it)).collect(Collectors.toSet()));
            }
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

    private Set<DatoIntervallEntitet> utledPerioder(ProsessTaskData prosessTaskData) {
        var perioderString = prosessTaskData.getPropertyValue(PERIODER);
        if (perioderString != null && !perioderString.isEmpty()) {
            return parseToPeriodeSet(perioderString);
        }

        return fallbackHåndtering(prosessTaskData);
    }

    TreeSet<DatoIntervallEntitet> parseToPeriodeSet(String perioderString) {
        return Arrays.stream(perioderString.split("\\|")).map(Periode::new).map(DatoIntervallEntitet::fra).collect(Collectors.toCollection(TreeSet::new));
    }

    private Set<DatoIntervallEntitet> fallbackHåndtering(ProsessTaskData prosessTaskData) {
        var fomString = prosessTaskData.getPropertyValue(PERIODE_FOM);
        var tomString = prosessTaskData.getPropertyValue(PERIODE_TOM);

        var fom = fomString != null ? LocalDate.parse(fomString) : null;
        var tom = tomString != null ? LocalDate.parse(tomString) : null;

        if (fom == null && tom == null) {
            return null;
        }
        validerPeriode(fom, tom);

        return Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
    }

    private void validerPeriode(LocalDate fom, LocalDate tom) {
        if (fom == null && tom != null) {
            throw new IllegalStateException("Ugyldig datorange, fom er null men ikke tom");
        }
        if (tom == null && fom != null) {
            throw new IllegalStateException("Ugyldig datorange, tom er null men ikke fom");
        }
    }

}
