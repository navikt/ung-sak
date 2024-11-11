package no.nav.ung.sak.domene.behandling.steg.kompletthet;

import static java.util.Collections.singletonList;
import static no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunktMedFrist;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.kompletthet.KompletthetResultat;

/**
 * Fellesklasse for gjenbrukte metode av subklasser for {@link VurderKompletthetSteg}.
 * <p>
 *     Favor composition over inheritance
 */
@Dependent
public class VurderKompletthetStegFelles {

    @Inject
    public VurderKompletthetStegFelles() {
    }

    public AksjonspunktResultat byggAutopunkt(KompletthetResultat kompletthetResultat, AksjonspunktDefinisjon apDef) {
        return opprettForAksjonspunktMedFrist(apDef, kompletthetResultat.getVenteårsak(), kompletthetResultat.getVentefrist());
    }

    public BehandleStegResultat evaluerUoppfylt(KompletthetResultat kompletthetResultat, AksjonspunktDefinisjon apDef) {
        if (kompletthetResultat.erFristUtløpt()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        AksjonspunktResultat autopunkt = byggAutopunkt(kompletthetResultat, apDef);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(singletonList(autopunkt));
    }

    public static boolean autopunktAlleredeUtført(AksjonspunktDefinisjon apDef, Behandling behandling) {
        return behandling.getAksjonspunktMedDefinisjonOptional(apDef)
            .map(Aksjonspunkt::erUtført)
            .orElse(Boolean.FALSE);
    }
}
