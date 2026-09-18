package no.nav.ung.ytelse.aktivitetspenger.del1.steg.andrelivsoppholdsytelser;

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

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_FAKTA_OM_ANDRE_LIVSOPPHOLDSYTELSER;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_FAKTA_OM_ANDRE_LIVSOPPHOLDSYTELSER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderFaktaOmAndreLivsoppholdsytelserSteg extends VurderFaktaOmVilkårSteg {

    VurderFaktaOmAndreLivsoppholdsytelserSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderFaktaOmAndreLivsoppholdsytelserSteg(BehandlingRepository behandlingRepository,
                                                      @Any Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere) {
        super(behandlingRepository, prosessTriggerPeriodeUtledere,
            BehandlingÅrsakType.ENDRET_LIVSOPPHOLDSYTELSE,
            AksjonspunktDefinisjon.VURDER_FAKTA_OM_ANDRE_LIVSOPPHOLDSYTELSER);
    }
}
