package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.beslutte;

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

@BehandlingStegRef(value = BehandlingStegType.NAV_KONTOR_BESLUTTER_VILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class NavKontorBeslutteVilkårSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private NavKontorBeslutteVilkårTjeneste navKontorBeslutteVilkårTjeneste;

    NavKontorBeslutteVilkårSteg() {
        // for CDI proxy
    }

    @Inject
    public NavKontorBeslutteVilkårSteg(BehandlingRepositoryProvider repositoryProvider,
                                       NavKontorBeslutteVilkårTjeneste navKontorBeslutteVilkårTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.navKontorBeslutteVilkårTjeneste = navKontorBeslutteVilkårTjeneste;
    }


    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return navKontorBeslutteVilkårTjeneste.besluttVilkår(kontekst, behandling);
    }


}
