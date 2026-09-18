package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.felles.VurderFaktaOmVilkårSteg;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_FAKTA_OM_BOSTED;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_FAKTA_OM_BOSTED)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderFaktaBostedSteg extends VurderFaktaOmVilkårSteg {

    VurderFaktaBostedSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaBostedSteg(BehandlingRepository behandlingRepository,
                                  @Any Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere) {
        super(behandlingRepository, prosessTriggerPeriodeUtledere,
            BehandlingÅrsakType.ENDRET_BOSTED,
            AksjonspunktDefinisjon.VURDER_FAKTA_OM_BOSTED);
    }
}
