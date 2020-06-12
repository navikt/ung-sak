package no.nav.k9.sak.behandling.prosessering;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.sak.behandling.prosessering.task.FortsettBehandlingTask;
import no.nav.k9.sak.behandling.prosessering.task.GjenopptaBehandlingTask;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.domene.registerinnhenting.EndringsresultatSjekker;
import no.nav.k9.sak.domene.registerinnhenting.RegisterdataEndringshåndterer;
import no.nav.k9.sak.domene.registerinnhenting.impl.OppfriskingAvBehandlingTask;
import no.nav.k9.sak.domene.registerinnhenting.task.DiffOgReposisjonerTask;
import no.nav.k9.sak.domene.registerinnhenting.task.InnhentIAYIAbakusTask;
import no.nav.k9.sak.domene.registerinnhenting.task.InnhentMedlemskapOpplysningerTask;
import no.nav.k9.sak.domene.registerinnhenting.task.InnhentPersonopplysningerTask;
import no.nav.k9.sak.domene.registerinnhenting.task.SettRegisterdataInnhentetTidspunktTask;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.log.mdc.MDCOperations;

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

    @Inject
    public BehandlingProsesseringTjenesteImpl(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                              RegisterdataEndringshåndterer registerdataEndringshåndterer,
                                              EndringsresultatSjekker endringsresultatSjekker,
                                              FagsakProsessTaskRepository fagsakProsessTaskRepository) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.registerdataEndringshåndterer = registerdataEndringshåndterer;
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

    // AV/PÅ Vent
    @Override
    public void taBehandlingAvVent(Behandling behandling) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
        behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
    }

    @Override
    public void settBehandlingPåVent(Behandling behandling, AksjonspunktDefinisjon apDef, LocalDateTime fristTid, Venteårsak venteårsak, String venteårsakVariant) {
        behandlingskontrollTjeneste.settBehandlingPåVent(behandling, apDef, behandling.getAktivtBehandlingSteg(), fristTid, venteårsak, venteårsakVariant);
    }

    // For snapshot av grunnlag før man gjør andre endringer enn registerinnhenting
    @Override
    public EndringsresultatSnapshot taSnapshotAvBehandlingsgrunnlag(Behandling behandling) {
        return endringsresultatSjekker.opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandling.getId());
    }

    // Returnerer endringer i grunnlag mellom snapshot og nåtilstand
    @Override
    public EndringsresultatDiff finnGrunnlagsEndring(Behandling behandling, EndringsresultatSnapshot før) {
        return endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(behandling.getId(), før);
    }

    @Override
    public void reposisjonerBehandlingVedEndringer(Behandling behandling, EndringsresultatDiff grunnlagDiff) {
        registerdataEndringshåndterer.reposisjonerBehandlingVedEndringer(behandling, grunnlagDiff);
    }

    @Override
    public ProsessTaskGruppe lagOppdaterFortsettTasksForPolling(Behandling behandling) {

        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new IllegalStateException("Utvikler feil: Kan ikke oppdater behandling med nye data når er allerede i iverksettelse/avsluttet. behandlingId=" + behandling.getId()
                + ", behandlingStatus=" + behandling.getStatus()
                + ", startpunkt=" + behandling.getStartpunkt()
                + ", resultat=" + behandling.getBehandlingResultatType());
        }

        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();

        ProsessTaskData registerdataOppdatererTask = new ProsessTaskData(OppfriskingAvBehandlingTask.TASKTYPE);
        registerdataOppdatererTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        gruppe.addNesteSekvensiell(registerdataOppdatererTask);
        if (skalHenteInnRegisterData(behandling)) {
            leggTilInnhentRegisterdataTasks(behandling, gruppe);
            var diffOgReposisjoner = new ProsessTaskData(DiffOgReposisjonerTask.TASKTYPE);
            diffOgReposisjoner.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
            try {
                diffOgReposisjoner.setPayload(JsonObjectMapper.getJson(endringsresultatSjekker.opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandling.getId())));
            } catch (IOException e) {
                throw new RuntimeException("Feil ved serialisering av snapshot", e);
            }
            gruppe.addNesteSekvensiell(diffOgReposisjoner);
        }
        ProsessTaskData fortsettBehandlingTask = new ProsessTaskData(FortsettBehandlingTask.TASKTYPE);
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
        ProsessTaskData taskData = new ProsessTaskData(FortsettBehandlingTask.TASKTYPE);
        Long fagsakId = behandling.getFagsakId();
        Long behandlingId = behandling.getId();
        taskData.setBehandling(fagsakId, behandlingId, behandling.getAktørId().getId());
        taskData.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
        return lagreMedCallId(fagsakId, behandlingId, taskData);
    }

    @Override
    public String opprettTasksForFortsettBehandlingGjenopptaStegNesteKjøring(Behandling behandling, BehandlingStegType behandlingStegType, LocalDateTime nesteKjøringEtter) {
        ProsessTaskData taskData = new ProsessTaskData(FortsettBehandlingTask.TASKTYPE);
        Long behandlingId = behandling.getId();
        Long fagsakId = behandling.getFagsakId();
        taskData.setBehandling(fagsakId, behandlingId, behandling.getAktørId().getId());
        taskData.setProperty(FortsettBehandlingTask.GJENOPPTA_STEG, behandlingStegType.getKode());
        if (nesteKjøringEtter != null) {
            taskData.setNesteKjøringEtter(nesteKjøringEtter);
        }
        return lagreMedCallId(fagsakId, behandlingId, taskData);
    }

    // Robust task til bruk ved gjenopptak fra vent (eller annen tilstand) (Hendelse: Manuell input, Frist utløpt, mv)
    @Override
    public void opprettTasksForGjenopptaOppdaterFortsett(Behandling behandling, boolean nyCallId) {

        Long fagsakId = behandling.getFagsakId();
        Long behandlingId = behandling.getId();
        if (behandling.erSaksbehandlingAvsluttet()) {
            throw new IllegalStateException("Utvikler-feil: kan ikke gjenoppta behandling når saksbehandling er avsluttet: behandlingId=" + behandlingId + ", status=" + behandling.getStatus());
        }

        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();
        String gjenopptaTaskType = GjenopptaBehandlingTask.TASKTYPE;
        var gjenopptaBehandlingTask = new ProsessTaskData(gjenopptaTaskType);
        var diffOgReposisjoner = new ProsessTaskData(DiffOgReposisjonerTask.TASKTYPE);
        var fortsettBehandlingTask = new ProsessTaskData(FortsettBehandlingTask.TASKTYPE);

        var eksisterendeGjenopptaTask = getEksisterendeTaskAvType(fagsakId, behandlingId, gjenopptaTaskType);
        if (eksisterendeGjenopptaTask.isPresent()) {
            log.warn("Har eksisterende task [{}], oppretter ikke nye for fagsakId={}, behandlingId={}: {}", gjenopptaTaskType, fagsakId, behandlingId, eksisterendeGjenopptaTask.get());
            return;
        }

        gjenopptaBehandlingTask.setBehandling(fagsakId, behandlingId, behandling.getAktørId().getId());
        gruppe.addNesteSekvensiell(gjenopptaBehandlingTask);
        if (skalHenteInnRegisterData(behandling)) {
            leggTilInnhentRegisterdataTasks(behandling, gruppe);
            diffOgReposisjoner.setBehandling(fagsakId, behandlingId, behandling.getAktørId().getId());
            try {
                diffOgReposisjoner.setPayload(JsonObjectMapper.getJson(endringsresultatSjekker.opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandlingId)));
            } catch (IOException e) {
                throw new RuntimeException("Feil ved serialisering av snapshot", e);
            }
            gruppe.addNesteSekvensiell(diffOgReposisjoner);
        }
        fortsettBehandlingTask.setBehandling(fagsakId, behandlingId, behandling.getAktørId().getId());
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
        gruppe.addNesteSekvensiell(fortsettBehandlingTask);
        if (nyCallId) {
            gruppe.setCallId(MDCOperations.generateCallId());
        } else {
            gruppe.setCallIdFraEksisterende();
        }
        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, gruppe);
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
        ProsessTaskData fortsettBehandlingTask = new ProsessTaskData(FortsettBehandlingTask.TASKTYPE);
        fortsettBehandlingTask.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        // NB: Viktig
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.GJENOPPTA_STEG, BehandlingStegType.INNHENT_REGISTEROPP.getKode());
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
        gruppe.addNesteSekvensiell(fortsettBehandlingTask);
        gruppe.setCallIdFraEksisterende();
        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(behandling.getFagsakId(), behandling.getId(), gruppe);
    }

    private void leggTilInnhentRegisterdataTasks(Behandling behandling, ProsessTaskGruppe gruppe) {
        ProsessTaskData innhentPersonopplysniger = new ProsessTaskData(InnhentPersonopplysningerTask.TASKTYPE);
        innhentPersonopplysniger.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        ProsessTaskData innhentMedlemskapOpplysniger = new ProsessTaskData(InnhentMedlemskapOpplysningerTask.TASKTYPE);
        innhentMedlemskapOpplysniger.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        ProsessTaskData abakusRegisterInnheting = new ProsessTaskData(InnhentIAYIAbakusTask.TASKTYPE);
        abakusRegisterInnheting.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());

        gruppe.addNesteParallell(innhentPersonopplysniger, innhentMedlemskapOpplysniger, abakusRegisterInnheting);

        ProsessTaskData oppdaterInnhentTidspunkt = new ProsessTaskData(SettRegisterdataInnhentetTidspunktTask.TASKTYPE);
        oppdaterInnhentTidspunkt.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        gruppe.addNesteSekvensiell(oppdaterInnhentTidspunkt);
    }

    private String lagreMedCallId(Long fagsakId, Long behandlingId, ProsessTaskData prosessTaskData) {
        var gruppe = new ProsessTaskGruppe(prosessTaskData);
        prosessTaskData.setCallIdFraEksisterende();
        return fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(fagsakId, behandlingId, gruppe);
    }

    @Override
    public void feilPågåendeTaskHvisFremtidigTaskEksisterer(Behandling behandling, Set<String> tasktyper) {
        if (tasktyper.isEmpty()) {
            return;
        }
        var pågår = fagsakProsessTaskRepository.sjekkStatusProsessTasks(behandling.getFagsakId(), behandling.getId(), null);
        Optional<ProsessTaskData> firstMatch = pågår.stream().filter(p -> tasktyper.contains(p.getTaskType())).findFirst();
        if (firstMatch.isPresent()) {
            throw new IllegalStateException("Kan ikke kjøre fordi annen task blokkerer: " + firstMatch.get());
        }
    }
}
