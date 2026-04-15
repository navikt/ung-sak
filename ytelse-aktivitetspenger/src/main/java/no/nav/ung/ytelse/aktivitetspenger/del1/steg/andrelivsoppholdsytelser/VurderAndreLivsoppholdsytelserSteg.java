package no.nav.ung.ytelse.aktivitetspenger.del1.steg.andrelivsoppholdsytelser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.vilkår.VilkårPeriodeFilterProvider;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.VilkårVurderingSteg;

import java.util.List;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_ANDRE_LIVSHOPPHOLDSYTELSER;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_ANDRE_LIVSHOPPHOLDSYTELSER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderAndreLivsoppholdsytelserSteg extends VilkårVurderingSteg {
    private VilkårResultatRepository vilkårResultatRepository;

    VurderAndreLivsoppholdsytelserSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderAndreLivsoppholdsytelserSteg(BehandlingModellRepository behandlingModellRepository,
                                              VilkårResultatRepository vilkårResultatRepository,
                                              BehandlingRepository behandlingRepository,
                                              @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                              VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider) {
        super(behandlingModellRepository, vilkårResultatRepository, behandlingRepository, vilkårsPerioderTilVurderingTjeneste, vilkårPeriodeFilterProvider);
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public VilkårType getAktuellVilkårType() {
        return VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR;
    }

    @Override
    public BehandleStegResultat utførResten(BehandlingskontrollKontekst kontekst) {
        if (vilkårResultatRepository.finnesRelevantPeriode(kontekst.getBehandlingId(), getAktuellVilkårType())) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_ANDRE_LIVSOPPHOLDSYTELSER));
        } else {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
    }

}
