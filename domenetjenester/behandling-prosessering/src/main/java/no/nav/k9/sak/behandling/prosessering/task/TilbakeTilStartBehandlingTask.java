package no.nav.k9.sak.behandling.prosessering.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/**
 * Kjører tilbakehopp til starten av prosessen. Brukes til rekjøring av saker som må gjøre alt på nytt.
 */
@ApplicationScoped
@ProsessTask(TilbakeTilStartBehandlingTask.TASKNAME)
// gruppeSekvens = false for å kunne hoppe tilbake ved feilende fortsettBehandling task
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class TilbakeTilStartBehandlingTask extends BehandlingProsessTask {

    private static final Logger log = LoggerFactory.getLogger(TilbakeTilStartBehandlingTask.class);
    public static final String TASKNAME = "behandlingskontroll.tilbakeTilStart";
    private BehandlingRepository behandlingRepository;
    private HistorikkRepository historikkRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private ProsesseringAsynkTjeneste prosesseringAsynkTjeneste;
    private FagsakProsessTaskRepository prosessTaskRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private OpptjeningRepository opptjeningRepository;

    TilbakeTilStartBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public TilbakeTilStartBehandlingTask(BehandlingRepository behandlingRepository,
                                         HistorikkRepository historikkRepository,
                                         ProsesseringAsynkTjeneste prosesseringAsynkTjeneste,
                                         BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                         FagsakProsessTaskRepository prosessTaskRepository,
                                         VilkårResultatRepository vilkårResultatRepository,
                                         OpptjeningRepository opptjeningRepository) {
        this.behandlingRepository = behandlingRepository;
        this.historikkRepository = historikkRepository;
        this.prosesseringAsynkTjeneste = prosesseringAsynkTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.opptjeningRepository = opptjeningRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        String behandlingId = prosessTaskData.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        logContext(behandling);

        var targetSteg = BehandlingStegType.START_STEG;
        var forventetPassertSteg = BehandlingStegType.START_STEG;

        if (!behandling.erSaksbehandlingAvsluttet() && behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandling.getId(), forventetPassertSteg)) {
            log.warn("Resetter behandling, flytter behandling tilbake fra {}, til {}.", behandling.getAktivtBehandlingSteg(), targetSteg);
            Long fagsakId = prosessTaskData.getFagsakId();
            BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);
            prosessTaskRepository.settFeiletTilSuspendert(fagsakId, behandling.getId());
            resetGrunnlag(behandling);
            hoppTilbake(behandling, targetSteg, kontekst);

        } else {
            log.warn("Kan ikke resette behandling. Behandling er avsluttet eller ikke kommet forbi {}, kan ikke hoppe tilbake til {}, så gjør ingenting.", forventetPassertSteg, targetSteg);
        }
    }

    private void resetGrunnlag(Behandling behandling) {
        var behandlingId = behandling.getId();
        vilkårResultatRepository.deaktiverVilkårsResultat(behandlingId);
        opptjeningRepository.deaktiverOpptjening(behandlingId);

        behandling.getOriginalBehandling().ifPresent(originalBehandling -> {
            Long originalId = originalBehandling.getId();
            opptjeningRepository.finnOpptjening(originalId).ifPresent(o -> opptjeningRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandling, behandling));
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
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setAktør(HistorikkAktør.VEDTAKSLØSNINGEN);
        historikkinnslag.setType(HistorikkinnslagType.SPOLT_TILBAKE);
        historikkinnslag.setBehandlingId(behandling.getId());

        String fraStegNavn = behandling.getAktivtBehandlingSteg() != null ? behandling.getAktivtBehandlingSteg().getNavn() : null;
        HistorikkInnslagTekstBuilder historieBuilder = new HistorikkInnslagTekstBuilder()
            .medHendelse(HistorikkinnslagType.SPOLT_TILBAKE)
            .medBegrunnelse("Behandlingen er flyttet fra " + fraStegNavn + " tilbake til " + tilStegNavn);
        historieBuilder.build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

}
