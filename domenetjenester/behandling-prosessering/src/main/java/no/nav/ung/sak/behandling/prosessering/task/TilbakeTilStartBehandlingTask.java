package no.nav.ung.sak.behandling.prosessering.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Kjører tilbakehopp til starten av prosessen. Brukes til rekjøring av saker som må gjøre alt på nytt.
 */
@ApplicationScoped
@ProsessTask(TilbakeTilStartBehandlingTask.TASKTYPE)
// gruppeSekvens = false for å kunne hoppe tilbake ved feilende fortsettBehandling task
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class TilbakeTilStartBehandlingTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "behandlingskontroll.tilbakeTilStart";
    public static final String PROPERTY_MANUELT_OPPRETTET = "manueltOpprettet";
    public static final String PROPERTY_START_STEG = "startSteg";
    private static final Logger log = LoggerFactory.getLogger(TilbakeTilStartBehandlingTask.class);
    private BehandlingRepository behandlingRepository;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;
    private FagsakProsessTaskRepository prosessTaskRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    TilbakeTilStartBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public TilbakeTilStartBehandlingTask(BehandlingRepository behandlingRepository,
                                         BehandlingLåsRepository behandlingLåsRepository,
                                         MottatteDokumentRepository mottatteDokumentRepository,
                                         HistorikkinnslagRepository historikkinnslagRepository,
                                         ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
                                         BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                         FagsakProsessTaskRepository prosessTaskRepository,
                                         VilkårResultatRepository vilkårResultatRepository) {
        super(behandlingLåsRepository);
        this.behandlingRepository = behandlingRepository;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        String behandlingId = prosessTaskData.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        logContext(behandling);

        var startSteg = BehandlingStegType.fraKode(prosessTaskData.getPropertyValue(PROPERTY_START_STEG));
        var targetSteg = (startSteg != null) ? startSteg : BehandlingStegType.START_STEG;
        var forventetPassertSteg = (startSteg != null) ? startSteg : BehandlingStegType.START_STEG;

        if (targetSteg != BehandlingStegType.START_STEG &&
            (erSammeStegEllerTidligere(behandling, startSteg, BehandlingStegType.INIT_VILKÅR) ||
                erSammeStegEllerTidligere(behandling, startSteg, BehandlingStegType.INIT_PERIODER))) {
            throw new IllegalStateException("Ikke implementert: Det er ikke støtte for å hoppe til steg før eller lik INIT_VILKÅR (med unntak av START_STEG.");
        }

        if (!behandling.erAvsluttet() && behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandling.getId(), forventetPassertSteg)) {
            log.warn("Resetter behandling, flytter behandling tilbake fra {}, til {}.", behandling.getAktivtBehandlingSteg(), targetSteg);
            Long fagsakId = prosessTaskData.getFagsakId();
            BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);

            if (Boolean.valueOf(prosessTaskData.getPropertyValue(PROPERTY_MANUELT_OPPRETTET))) {
                BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ANNET).medManueltOpprettet(true).buildFor(behandling);
                behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
            }

            Set<FagsakYtelseType> rammevedtak_typer = Set.of(FagsakYtelseType.OMSORGSPENGER_KS, FagsakYtelseType.OMSORGSPENGER_MA, FagsakYtelseType.OMSORGSPENGER_AO);
            boolean erRammevedtak = rammevedtak_typer.contains(behandling.getFagsakYtelseType()); //rammevedtak-behandlinger oppdaterer ikke status på dokumentene
            if (harMottatteDokumenterTilBehandling(behandling) && !erRammevedtak) {
                log.warn("Kan ikke hoppe tilbake når det er mottatte dokumenter som ikke har blitt behandlet ferdig.");
                return; //ønsker ikke retry fordi premisset for opprettelse av tasken sannsynligvis ikke er gyldig lenger etter at dokumentet er ferdig mottatt
            }

            prosessTaskRepository.settFeiletTilSuspendert(fagsakId, behandling.getId());

            if (startSteg == BehandlingStegType.START_STEG) {
                resetGrunnlag(behandling);
            }
            hoppTilbake(behandling, targetSteg, kontekst);

        } else {
            log.warn("Kan ikke resette behandling. Behandling er avsluttet eller ikke kommet forbi {}, kan ikke hoppe tilbake til {}, så gjør ingenting.", forventetPassertSteg, targetSteg);
        }
    }

    private boolean harMottatteDokumenterTilBehandling(Behandling behandling) {
        return !mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(behandling.getFagsakId(), DokumentStatus.BEHANDLER, DokumentStatus.MOTTATT).isEmpty();
    }

    private boolean erSammeStegEllerTidligere(Behandling behandling, BehandlingStegType steg, BehandlingStegType sjekkMotSteg) {
        return behandlingskontrollTjeneste.sammenlignRekkefølge(behandling.getFagsakYtelseType(), behandling.getType(), steg, sjekkMotSteg) <= 0;
    }

    private void resetGrunnlag(Behandling behandling) {
        var behandlingId = behandling.getId();
        vilkårResultatRepository.deaktiverVilkårsResultat(behandlingId);

        behandling.getOriginalBehandlingId().ifPresent(originalId -> {
            vilkårResultatRepository.kopier(originalId, behandlingId);
        });

    }

    private void hoppTilbake(Behandling behandling, BehandlingStegType tilSteg, BehandlingskontrollKontekst kontekst) {
        doHoppTilSteg(behandling, kontekst, tilSteg);
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
        prosesseringAsynkTjeneste.asynkProsesserBehandlingMergeGruppe(behandling);
    }

    private void doHoppTilSteg(Behandling behandling, BehandlingskontrollKontekst kontekst, BehandlingStegType tilSteg) {
        behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        lagHistorikkinnslag(behandling, tilSteg.getNavn());

        behandlingskontrollTjeneste.behandlingTilbakeføringTilTidligereBehandlingSteg(kontekst, tilSteg);
    }

    private void lagHistorikkinnslag(Behandling behandling, String tilStegNavn) {
        String fraStegNavn = behandling.getAktivtBehandlingSteg() != null ? behandling.getAktivtBehandlingSteg().getNavn() : null;
        var historikkinnslagBuilder = new Historikkinnslag.Builder();
        historikkinnslagBuilder.medAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslagBuilder.medBehandlingId(behandling.getId());
        historikkinnslagBuilder.medFagsakId(behandling.getFagsakId());
        historikkinnslagBuilder.medTittel("Behandlingen er automatisk flyttet");
        historikkinnslagBuilder.addLinje("Behandlingen er flyttet fra " + fraStegNavn + "  tilbake til " + tilStegNavn);
        historikkinnslagRepository.lagre(historikkinnslagBuilder.build());
    }

}
