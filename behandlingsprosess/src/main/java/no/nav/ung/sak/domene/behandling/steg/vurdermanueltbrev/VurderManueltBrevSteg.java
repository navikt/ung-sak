package no.nav.ung.sak.domene.behandling.steg.vurdermanueltbrev;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_MANUELT_BREV;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.ung.sak.behandling.prosessering.task.FortsettBehandlingTask;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;

@BehandlingStegRef(value = VURDER_MANUELT_BREV)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderManueltBrevSteg implements BehandlingSteg {

    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    VurderManueltBrevSteg() {
        //for CDI proxy
    }

    @Inject
    public VurderManueltBrevSteg(FagsakProsessTaskRepository fagsakProsessTaskRepository) {
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        //opprett task for å umiddelbart fortesette behandlingen etter at den settes på vent (se under)
        lagTaskForÅTaAvVent(kontekst);
        //settes på vent for å tvinge avslutning av transaksjon
        //slik at alle nødvendige data blir tilgjengelig for k9-formidling ved kall tilbake
        return BehandleStegResultat.settPåVent();
    }

    @Override
    public BehandleStegResultat gjenopptaSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void lagTaskForÅTaAvVent(BehandlingskontrollKontekst kontekst) {
        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();

        ProsessTaskData fortsettBehandlingTask =  ProsessTaskData.forProsessTask(FortsettBehandlingTask.class);
        fortsettBehandlingTask.setBehandling(kontekst.getFagsakId(), kontekst.getBehandlingId(), kontekst.getAktørId().getId());
        // NB: Viktig
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.GJENOPPTA_STEG, VURDER_MANUELT_BREV.getKode());
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
        gruppe.addNesteSekvensiell(fortsettBehandlingTask);

        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(kontekst.getFagsakId(), kontekst.getBehandlingId(), gruppe);
    }

}
