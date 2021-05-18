package no.nav.k9.sak.domene.behandling.steg.preforeslåvedtak;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.sak.behandling.prosessering.task.FortsettBehandlingTask;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;

@BehandlingStegRef(kode = "VURDER_MANUELT_BREV")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderManueltBrevSteg implements BehandlingSteg {

    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private Boolean lansert;

    VurderManueltBrevSteg() {
        //for CDI proxy
    }

    @Inject
    public VurderManueltBrevSteg(FagsakProsessTaskRepository fagsakProsessTaskRepository,
                                 @KonfigVerdi(value = "FORMIDLING_RETUR_MALTYPER", defaultVerdi = "true") Boolean lansert) {
        this.fagsakProsessTaskRepository = fagsakProsessTaskRepository;
        this.lansert = lansert;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        if (!lansert){
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        //opprett task for å umiddelbart fortesette behandlingen etter at den settes på vent (se under)
        lagTaskForÅTaAvVent(kontekst);
        //settes på vent for å tvinge avslutning av transaksjon
        //slik at alle nødvendige data blir tilgjengelig for k9-formidling ved kall tilbake
        return BehandleStegResultat.settPåVent();
    }

    @Override
    public BehandleStegResultat gjenopptaSteg(BehandlingskontrollKontekst kontekst) {
        if (!lansert){
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        var maltyper = hentTilgjengeligeMaltyper(kontekst.getBehandlingId());
        return utledAksjonspunkt(maltyper);
    }

    private void lagTaskForÅTaAvVent(BehandlingskontrollKontekst kontekst) {
        ProsessTaskGruppe gruppe = new ProsessTaskGruppe();

        ProsessTaskData fortsettBehandlingTask = new ProsessTaskData(FortsettBehandlingTask.TASKTYPE);
        fortsettBehandlingTask.setBehandling(kontekst.getFagsakId(), kontekst.getBehandlingId(), kontekst.getAktørId().getId());
        // NB: Viktig
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.GJENOPPTA_STEG, BehandlingStegType.VURDER_MANUELT_BREV.getKode());
        fortsettBehandlingTask.setProperty(FortsettBehandlingTask.MANUELL_FORTSETTELSE, String.valueOf(true));
        gruppe.addNesteSekvensiell(fortsettBehandlingTask);

        fagsakProsessTaskRepository.lagreNyGruppeKunHvisIkkeAlleredeFinnesOgIngenHarFeilet(kontekst.getFagsakId(), kontekst.getBehandlingId(), gruppe);
    }

    private BehandleStegResultat utledAksjonspunkt(Void maltyper) {
        return trengerFritekstbrev(maltyper)
            ? BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_MANUELT_BREV))
            : BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean trengerFritekstbrev(Void maltyper) {
        //TODO riktig utledning
        return false;
    }

    private Void hentTilgjengeligeMaltyper(Long behandlingId) {
        //TODO kall på k9-formidling
        return null;
    }
}
