package no.nav.k9.sak.ytelse.frisinn.filter;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VARIANT_FILTER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregnytelse.BeregneYtelseSteg;

@FagsakYtelseTypeRef(FRISINN)
@BehandlingStegRef(stegtype = VARIANT_FILTER)
@BehandlingTypeRef
@ApplicationScoped
public class  FiltrerUtVariantSomIkkeStøttesSteg implements BeregneYtelseSteg {

    @Inject
    public FiltrerUtVariantSomIkkeStøttesSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
