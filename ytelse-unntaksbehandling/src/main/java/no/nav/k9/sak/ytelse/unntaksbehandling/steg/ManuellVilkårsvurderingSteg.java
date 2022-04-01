package no.nav.k9.sak.ytelse.unntaksbehandling.steg;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.MANUELL_VILKÅRSVURDERING;
import static no.nav.k9.kodeverk.behandling.BehandlingType.UNNTAKSBEHANDLING;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

@FagsakYtelseTypeRef
@BehandlingStegRef(stegtype = MANUELL_VILKÅRSVURDERING)
@BehandlingTypeRef(UNNTAKSBEHANDLING)
@ApplicationScoped
public class ManuellVilkårsvurderingSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;

    public ManuellVilkårsvurderingSteg() {
        // CDO
    }

    @Inject
    public ManuellVilkårsvurderingSteg(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        if (BehandlingResultatType.INNVILGET.equals(behandling.getBehandlingResultatType())) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.MANUELL_TILKJENT_YTELSE));
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
