package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bistandsvilkår;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.felles.VurderFaktaOmVilkårSteg;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_FAKTA_OM_BISTAND;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_FAKTA_OM_BISTAND)
@BehandlingTypeRef(BehandlingType.REVURDERING)
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderFaktaOmBistandsvilkårSteg extends VurderFaktaOmVilkårSteg {

    VurderFaktaOmBistandsvilkårSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmBistandsvilkårSteg(BehandlingRepository behandlingRepository,
                                           @Any Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere) {
        super(behandlingRepository, prosessTriggerPeriodeUtledere,
            BehandlingÅrsakType.ENDRET_BISTANDSBEHOV,
            AksjonspunktDefinisjon.VURDER_FAKTA_OM_BISTAND);
    }
}
