package no.nav.ung.ytelse.aktivitetspenger.del1.steg.beslutte;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

@BehandlingStegRef(value = BehandlingStegType.LOKALKONTOR_BESLUTTER_VILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class LokalkontorBeslutteVilkårSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private LokalkontorBeslutteVilkårTjeneste lokalkontroBeslutteVilkårTjeneste;

    LokalkontorBeslutteVilkårSteg() {
        // for CDI proxy
    }

    @Inject
    public LokalkontorBeslutteVilkårSteg(BehandlingRepositoryProvider repositoryProvider,
                                         LokalkontorBeslutteVilkårTjeneste lokalkontroBeslutteVilkårTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.lokalkontroBeslutteVilkårTjeneste = lokalkontroBeslutteVilkårTjeneste;
    }


    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return lokalkontroBeslutteVilkårTjeneste.besluttVilkår(kontekst, behandling);
    }


}
