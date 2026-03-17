package no.nav.ung.ytelse.aktivitetspenger.del1.steg.foreslåvlkår;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import org.slf4j.Logger;

@BehandlingStegRef(value = BehandlingStegType.LOKALKONTOR_FORESLÅ_VILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class LokalkontorForeslsåVilkårSteg implements BehandlingSteg {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(LokalkontorForeslsåVilkårSteg.class);

    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;

    LokalkontorForeslsåVilkårSteg() {
        // for CDI proxy
    }

    @Inject
    public LokalkontorForeslsåVilkårSteg(BehandlingAnsvarligRepository behandlingAnsvarligRepository) {
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
    }


    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();

        if (!behandlingAnsvarligRepository.erTotrinnsBehandling(behandlingId, BehandlingDel.LOKAL)) {
            behandlingAnsvarligRepository.setToTrinnsbehandling(behandlingId, BehandlingDel.LOKAL);
            logger.info("To-trinn satt på behandling={} for LOKAL del", behandlingId);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }


}
