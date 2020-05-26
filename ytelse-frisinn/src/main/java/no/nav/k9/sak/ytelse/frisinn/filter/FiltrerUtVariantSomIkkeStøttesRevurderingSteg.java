package no.nav.k9.sak.ytelse.frisinn.filter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.beregnytelse.BeregneYtelseSteg;

@FagsakYtelseTypeRef("FRISINN")
@BehandlingStegRef(kode = "VARIANT_FILTER")
@BehandlingTypeRef("BT-004")
@ApplicationScoped
public class FiltrerUtVariantSomIkkeStøttesRevurderingSteg implements BeregneYtelseSteg {
    @SuppressWarnings("unused")
    private BehandlingRepository behandlingRepository;

    FiltrerUtVariantSomIkkeStøttesRevurderingSteg() {
        // for CDI proxy
    }

    @Inject
    public FiltrerUtVariantSomIkkeStøttesRevurderingSteg(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
