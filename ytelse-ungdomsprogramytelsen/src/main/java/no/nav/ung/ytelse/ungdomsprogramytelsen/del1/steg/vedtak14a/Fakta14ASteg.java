package no.nav.ung.ytelse.ungdomsprogramytelsen.del1.steg.vedtak14a;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;

import java.util.List;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.FAKTA_14A_VEDTAK;

@ApplicationScoped
@BehandlingStegRef(value = FAKTA_14A_VEDTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class Fakta14ASteg implements BehandlingSteg {

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        //TODO ved avslag på foregående vilkår for alle perioder kan perioder settes til ikke vurdert
        return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.FAKTA_14A_VEDTAK));
    }

}
