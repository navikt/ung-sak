package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import no.nav.folketrygdloven.beregningsgrunnlag.resultat.BeregningAksjonspunktResultat;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;

public class BeregningResultatMapper {

    public static AksjonspunktResultat map(BeregningAksjonspunktResultat beregningResultat) {
        if (beregningResultat.harFrist()) {
            return AksjonspunktResultat.opprettForAksjonspunktMedFrist(
                AksjonspunktDefinisjon.fraKode(beregningResultat.getBeregningAksjonspunktDefinisjon().getKode()),
                Venteårsak.fraKode(beregningResultat.getVenteårsak().getKode()),
                beregningResultat.getVentefrist());
        }
        return AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.fraKode(beregningResultat.getBeregningAksjonspunktDefinisjon().getKode()));
    }
}
