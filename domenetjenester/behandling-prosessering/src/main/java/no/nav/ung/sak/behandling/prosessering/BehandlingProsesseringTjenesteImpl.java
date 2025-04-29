package no.nav.ung.sak.behandling.prosessering;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.request.RegisterdataType;
import no.nav.k9.felles.log.mdc.MDCOperations;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.TaskType;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.prosessering.task.FortsettBehandlingTask;
import no.nav.ung.sak.behandling.prosessering.task.GjenopptaBehandlingTask;
import no.nav.ung.sak.behandling.prosessering.task.HoppTilbakeTilStegTask;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.ung.sak.domene.registerinnhenting.EndringsresultatSjekker;
import no.nav.ung.sak.domene.registerinnhenting.InformasjonselementerUtleder;
import no.nav.ung.sak.domene.registerinnhenting.RegisterdataEndringshåndterer;
import no.nav.ung.sak.domene.registerinnhenting.impl.OppfriskingAvBehandlingTask;
import no.nav.ung.sak.domene.registerinnhenting.task.*;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Grensesnitt for å kjøre behandlingsprosess, herunder gjenopptak, registeroppdatering, koordinering av sakskompleks mv.
 * Alle kall til utføringsmetode i behandlingskontroll bør gå gjennom tasks opprettet her.
 * Merk Dem:
 * - ta av vent og grunnlagsoppdatering kan føre til reposisjonering av behandling til annet steg
 * - grunnlag endres ved ankomst av dokument, ved registerinnhenting og ved senere overstyring ("bekreft AP" eller egne overstyringAP)
 * - Hendelser: Ny behandling (Manuell, dokument, mv), Gjenopptak (Manuell/Frist), Interaktiv (Oppdater/Fortsett), Dokument, Datahendelse,
 * Vedtak, KØ-hendelser
 **/
@Dependent
public class BehandlingProsesseringTjenesteImpl implements BehandlingProsesseringTjeneste {
    private static final Logger log = LoggerFactory.getLogger(BehandlingProsesseringTjenesteImpl.class);

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private RegisterdataEndringshåndterer registerdataEndringshåndterer;
    private EndringsresultatSjekker endringsresultatSjekker;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    private Instance<InformasjonselementerUtleder> informasjonselementer;

    private Instance<EndringStartpunktUtleder> startpunktUtledere;

    @Inject
    public BehandlingProsesseringTjenesteImpl(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                              RegisterdataEndringshåndterer registerdataEndringshåndterer,
                                              @Any Instance<InformasjonselementerUtleder> informasjonselementer,
                                              @Any Instance<EndringStartpunktUtleder> startpunktUtledere,
                                              EndringsresultatSjekker endringsresultatSjekker,
                                              FagsakProsessTaskRepository fagsakProsessTaskRepository) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.registerdataEndringshåndterer = registerdataEndringshåndterer;
        this.informasjonselementer = informasjonselementer;
        this.startpunktUtledere = startpunktUtledere;
        this.endringsresultatSjekker = endringsresultatSjekker;
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
    }

    public BehandlingProsesseringTjenesteImpl() {
        // NOSONAR
    }

    @Override
    public boolean skalInnhenteRegisteropplysningerPåNytt(Behandling behandling) {
        return registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);
    }

    @Override
    public void tvingInnhentingRegisteropplysninger(Behandling behandling) {
        registerdataEndringshåndterer.sikreInnhentingRegisteropplysningerVedNesteOppdatering(behandling);
    }

    @Override
    public ProsessTaskGruppe lagOppdaterFortsettTasksForPolling(Behandling behandling) {
        boolean innhentRegisterdata = skalHenteInnRegisterData(behandling);
        if (innhentRegisterdata) {
            log.info("Innhenter registerdata på nytt, grunnlg er utdatert");
        }
        return doOppfriskingTaskOgFortsattBehandling(behandling, innhentRegisterdata);
    }

    @Override
    public ProsessTaskGruppe lagOppdaterFortsettTasksForPolling(Behandling behandling, boolean forceInnhent) {
        if (forceInnhent) {
            log.warn("Innhenter registerdata på nytt (force), selv om data er hentet tidligere i dag");
            return doOppfriskingTaskOgFortsattBehandling(behandling, forceInnhent);
        }
        return lagOppdaterFortsettTasksForPolling(behandling);
    }

    private ProsessTaskGruppe doOppfriskingTaskOgFortsattBehandling(Behandling behandling, boolean innhentRegisterdataFørst) {
        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new IllegalStateException("Utvikler feil: Kan ikke oppdater behandling med nye data når er allerede i iverksettelse/avsluttet. behandlingId=" + behandling.getId()
                + ", behandlingStatus=" + behandling.getStatus()
                + ", startpunkt=" + behandling.getStartpunkt()
                + ", resultat=" + behandling.getBehandlingResultatType());
        }

        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();

        ProsessTaskData registerdataOppdatererTask = ProsessTaskData.forProsessTask(OppfriskingAvBehandlingTask.class);
        registerdataOppdatererTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        gruppe.addNesteSekvensiell(registerdataOppdatererTask);
        if (innhentRegisterdataFørst) {
            log.info("Innhenter registerdata på nytt for å sjekke endringer for behandling: {}", behandling.getId());
            leggTilTasksForInnhentRegisterdataPåNytt(behandling, gruppe, true);
        } else {
            log.info("Sjekker om det har tilkommet nye søknader/inntektsmeldinger og annet for behandling: {}", behandling.getId());
            leggTilTaskForDiffOgReposisjoner(behandling, gruppe, true);
        }
        ProsessTaskData fortsettBehandlingTask = ProsessTaskData.forProsessTask(FortsettBehandlingTask.class);
        fortsettBehandlingTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
        gruppe.addNesteSekvensiell(fortsettBehandlingTask);
        gruppe.setCallIdFraEksisterende();
        return gruppe;
    }

    private boolean skalHenteInnRegisterData(Behandling behandling) {
        if (behandling.erSaksbehandlingAvsluttet() || !behandling.erYtelseBehandling()) {
            return false;
        }

        return behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.INNHENT_REGISTEROPP)
            && registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);
    }

    // Til bruk ved gjenopptak fra vent (Hendelse: Manuell input, Frist utløpt, mv)
    @Override
    public String opprettTasksForFortsettBehandling(Behandling behandling) {
        ProsessTaskData taskData = ProsessTaskData.forProsessTask(FortsettBehandlingTask.class);
        Long fagsakId = behandling.getFagsakId();
        Long behandlingId = behandling.getId();
        taskData.setBehandling(fagsakId, behandlingId, behandling.getAktørId().getId());
        taskData.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
        return lagreMedCallId(fagsakId, behandlingId, taskData);
    }

    @Override
    public String opprettTasksForFortsettBehandlingGjenopptaStegNesteKjøring(Behandling behandling, BehandlingStegType behandlingStegType, LocalDateTime nesteKjøringEtter) {
        ProsessTaskData taskData = ProsessTaskData.forProsessTask(FortsettBehandlingTask.class);
        Long behandlingId = behandling.getId();
        Long fagsakId = behandling.getFagsakId();
        taskData.setBehandling(fagsakId, behandlingId, behandling.getAktørId().getId());
        taskData.setProperty(FortsettBehandlingTask.GJENOPPTA_STEG, behandlingStegType.getKode());
        if (nesteKjøringEtter != null) {
            taskData.setNesteKjøringEtter(nesteKjøringEtter);
        }
        return lagreMedCallId(fagsakId, behandlingId, taskData);
    }

    public String opprettTasksForÅHoppeTilbakeTilGittStegOgFortsettDerfra(Behandling behandling, BehandlingStegType behandlingStegType) {
        Objects.requireNonNull(behandlingStegType);

        Long behandlingId = behandling.getId();
        Long fagsakId = behandling.getFagsakId();
        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();

        ProsessTaskData hoppTilbakeTask = ProsessTaskData.forProsessTask(HoppTilbakeTilStegTask.class);
        hoppTilbakeTask.setBehandling(fagsakId, behandlingId, behandling.getAktørId().getId());
        hoppTilbakeTask.setProperty(HoppTilbakeTilStegTask.PROPERTY_TIL_STEG, behandlingStegType.getKode());

        gruppe.addNesteSekvensiell(hoppTilbakeTask);

        ProsessTaskData fortsettTask = ProsessTaskData.forProsessTask(FortsettBehandlingTask.class);
        fortsettTask.setBehandling(fagsakId, behandlingId, behandling.getAktørId().getId());
        gruppe.addNesteSekvensiell(fortsettTask);

        gruppe.setCallIdFraEksisterende();
        return fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, gruppe);
    }

    // Robust task til bruk ved gjenopptak fra vent (eller annen tilstand) (Hendelse: Manuell input, Frist utløpt, mv)
    @Override
    public void opprettTasksForGjenopptaOppdaterFortsett(Behandling behandling, boolean nyCallId) {
        var gruppe = opprettTaskGruppeForGjenopptaOppdaterFortsett(behandling, nyCallId, false, false);

        if (gruppe == null) {
            return;
        }
        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(behandling.getFagsakId(), behandling.getId(), gruppe);
    }

    @Override
    public void opprettTasksForGjenopptaOppdaterFortsett(Behandling behandling, boolean nyCallId, boolean forceregisterinnhenting) {
        BehandlingProsessTask.logContext(behandling);

        Long fagsakId = behandling.getFagsakId();
        Long behandlingId = behandling.getId();
        ProsessTaskGruppe gruppe = opprettTaskGruppeForGjenopptaOppdaterFortsett(behandling, nyCallId, true, forceregisterinnhenting);
        if (gruppe == null) {
            return;
        }
        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, gruppe);
    }

    @Override
    public ProsessTaskGruppe opprettTaskGruppeForGjenopptaOppdaterFortsett(Behandling behandling, boolean nyCallId, boolean skalUtledeÅrsaker) {
        return opprettTaskGruppeForGjenopptaOppdaterFortsett(behandling, nyCallId, skalUtledeÅrsaker, true);
    }

    @Override
    public ProsessTaskGruppe opprettTaskGruppeForGjenopptaOppdaterFortsett(Behandling behandling, boolean nyCallId, boolean skalUtledeÅrsaker, boolean forceInnhentingAvRegisterdata) {
        Long fagsakId = behandling.getFagsakId();
        Long behandlingId = behandling.getId();

        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new IllegalStateException("Utvikler-feil: kan ikke gjenoppta behandling når saksbehandling er avsluttet: behandlingId=" + behandlingId + ", status=" + behandling.getStatus());
        }

        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
        var gjenopptaTaskType = GjenopptaBehandlingTask.TASKTYPE;
        var gjenopptaBehandlingTask = ProsessTaskData.forProsessTask(GjenopptaBehandlingTask.class);

        var eksisterendeGjenopptaTask = getEksisterendeTaskAvType(fagsakId, behandlingId, gjenopptaTaskType);
        if (eksisterendeGjenopptaTask.isPresent()) {
            log.warn("Har eksisterende task [{}], oppretter ikke nye for fagsakId={}, behandlingId={}: {}", gjenopptaTaskType, fagsakId, behandlingId, eksisterendeGjenopptaTask.get());
            return null;
        }

        gjenopptaBehandlingTask.setBehandling(fagsakId, behandlingId, behandling.getAktørId().getId());
        gruppe.addNesteSekvensiell(gjenopptaBehandlingTask);

        if (forceInnhentingAvRegisterdata || skalHenteInnRegisterData(behandling)) {
            log.info("Innhenter registerdata på nytt for å sjekke endringer for behandling: {}", behandlingId);
            leggTilTasksForInnhentRegisterdataPåNytt(behandling, gruppe, skalUtledeÅrsaker);
        } else {
            log.info("Sjekker om det har tilkommet nye inntektsmeldinger for behandling: {}", behandlingId);
            leggTilTaskForDiffOgReposisjoner(behandling, gruppe, skalUtledeÅrsaker);
        }

        var fortsettBehandlingTask = ProsessTaskData.forProsessTask(FortsettBehandlingTask.class);
        fortsettBehandlingTask.setBehandling(fagsakId, behandlingId, behandling.getAktørId().getId());
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
        gruppe.addNesteSekvensiell(fortsettBehandlingTask);
        if (nyCallId) {
            gruppe.setCallId(MDCOperations.generateCallId());
        } else {
            gruppe.setCallIdFraEksisterende();
        }
        return gruppe;
    }

    private void leggTilTasksForInnhentRegisterdataPåNytt(Behandling behandling, ProsessTaskGruppe gruppe, boolean skalUtledeÅrsaker) {
        leggTilInnhentRegisterdataTasks(behandling, gruppe);
        leggTilTaskForDiffOgReposisjoner(behandling, gruppe, skalUtledeÅrsaker);
    }

    private void leggTilTaskForDiffOgReposisjoner(Behandling behandling, ProsessTaskGruppe gruppe, boolean skalUtledeÅrsaker) {
        var diffOgReposisjoner = ProsessTaskData.forProsessTask(DiffOgReposisjonerTask.class);
        diffOgReposisjoner.setProperty(DiffOgReposisjonerTask.UTLED_ÅRSAKER, "" + skalUtledeÅrsaker);
        diffOgReposisjoner.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        try {
            var snapshotFørInnhenting = endringsresultatSjekker.opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandling.getId());
            diffOgReposisjoner.setPayload(JsonObjectMapper.getJson(snapshotFørInnhenting));
        } catch (IOException e) {
            throw new RuntimeException("Feil ved serialisering av snapshot", e);
        }
        gruppe.addNesteSekvensiell(diffOgReposisjoner);
    }

    private Optional<ProsessTaskData> getEksisterendeTaskAvType(Long fagsakId, Long behandlingId, String taskType) {
        var åpneTasks = fagsakProsessTaskRepository.finnAlleÅpneTasksForAngittSøk(fagsakId, behandlingId, null);
        var task = åpneTasks.stream().filter(t -> Objects.equals(t.getTaskType(), taskType)).findFirst();
        return task;
    }

    @Override
    public void opprettTasksForInitiellRegisterInnhenting(Behandling behandling) {
        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();

        leggTilInnhentRegisterdataTasks(behandling, gruppe);

        // Starter opp prosessen igjen fra steget hvor den var satt på vent
        ProsessTaskData fortsettBehandlingTask = ProsessTaskData.forProsessTask(FortsettBehandlingTask.class);
        fortsettBehandlingTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        // NB: Viktig
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.GJENOPPTA_STEG, BehandlingStegType.INNHENT_REGISTEROPP.getKode());
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
        gruppe.addNesteSekvensiell(fortsettBehandlingTask);
        gruppe.setCallIdFraEksisterende();
        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(behandling.getFagsakId(), behandling.getId(), gruppe);
    }

    private void leggTilInnhentRegisterdataTasks(Behandling behandling, ProsessTaskGruppe gruppe) {

        if (endringsresultatSjekker.harStatusOpprettetEllerUtredes(behandling)) {
            var taskTyper = utledRegisterinnhentingTaskTyper(behandling);

            var tasks = taskTyper.stream()
                .map(TaskType::new)
                .map(it -> mapTilTask(it, behandling))
                .collect(Collectors.toList());

            if (tasks.isEmpty()) {
                throw new UnsupportedOperationException("Utvikler-feil: Håpet på å hente inn noe registerdata for ytelseType=" + behandling.getFagsakYtelseType());
            }

            gruppe.addNesteParallell(tasks);
            log.info("Henter inn registerdata: {}", gruppe.getTasks().stream().map(ProsessTaskGruppe.Entry::getTask).map(ProsessTaskData::getTaskType).collect(Collectors.toList()));
        }

        ProsessTaskData oppdaterInnhentTidspunkt = ProsessTaskData.forProsessTask(SettRegisterdataInnhentetTidspunktTask.class);
        oppdaterInnhentTidspunkt.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        gruppe.addNesteSekvensiell(oppdaterInnhentTidspunkt);
    }

    private ProsessTaskData mapTilTask(TaskType taskType, Behandling behandling) {
        var task = ProsessTaskData.forTaskType(taskType);
        task.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        return task;
    }

    @Override
    public List<String> utledRegisterinnhentingTaskTyper(Behandling behandling) {
        var tasks = new ArrayList<String>();
        // Rekkefølgen her her viktig
        // Innhenting av ungdomsprogramperioder må komme før annen innhenting siden denne påvirker opplysningsperioden
        EndringStartpunktUtleder.finnUtleder(startpunktUtledere, UngdomsprogramPeriodeGrunnlag.class, behandling.getFagsakYtelseType())
            .ifPresent(u -> tasks.add(InnhentUngdomsprogramperioderTask.TASKTYPE));

        EndringStartpunktUtleder.finnUtleder(startpunktUtledere, PersonInformasjonEntitet.class, behandling.getFagsakYtelseType())
            .ifPresent(u -> tasks.add(InnhentPersonopplysningerTask.TASKTYPE));

        EndringStartpunktUtleder.finnUtleder(startpunktUtledere, InntektArbeidYtelseGrunnlag.class, behandling.getFagsakYtelseType()).ifPresent(u -> {
            if (skalInnhenteAbakus(behandling)) {
                tasks.add(InnhentIAYIAbakusTask.TASKTYPE);
            }
        });
        return tasks;
    }

    private boolean skalInnhenteAbakus(Behandling behandling) {
        if (behandling.getBehandlingÅrsakerTyper().contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT)) {
            var informasjonselementerUtleder = finnTjeneste(behandling.getFagsakYtelseType(), behandling.getType());
            Set<RegisterdataType> registerdata = informasjonselementerUtleder.utled(behandling.getType());
            return registerdata != null && !(registerdata.isEmpty());
        }
        return false;
    }

    private InformasjonselementerUtleder finnTjeneste(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return InformasjonselementerUtleder.finnTjeneste(informasjonselementer, ytelseType, behandlingType);
    }

    private String lagreMedCallId(Long fagsakId, Long behandlingId, ProsessTaskData prosessTaskData) {
        var gruppe = new ProsessTaskGruppe(prosessTaskData);
        prosessTaskData.setCallIdFraEksisterende();
        return fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, gruppe);
    }


}
