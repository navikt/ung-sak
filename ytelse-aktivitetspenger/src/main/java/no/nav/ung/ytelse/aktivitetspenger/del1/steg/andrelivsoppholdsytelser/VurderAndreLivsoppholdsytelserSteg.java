package no.nav.ung.ytelse.aktivitetspenger.del1.steg.andrelivsoppholdsytelser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.vilkår.VilkårPeriodeFilterProvider;
import no.nav.ung.sak.vilkår.ManuelleVilkårRekkefølgeTjeneste;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import no.nav.ung.sak.vilkår.VilkårVurderingSteg;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_ANDRE_LIVSHOPPHOLDSYTELSER;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_ANDRE_LIVSHOPPHOLDSYTELSER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public class VurderAndreLivsoppholdsytelserSteg extends VilkårVurderingSteg {
    private ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    VurderAndreLivsoppholdsytelserSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderAndreLivsoppholdsytelserSteg(ManuelleVilkårRekkefølgeTjeneste manuelleVilkårRekkefølgeTjeneste,
                                              VilkårResultatRepository vilkårResultatRepository,
                                              VilkårTjeneste vilkårTjeneste,
                                              BehandlingRepository behandlingRepository,
                                              @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                              VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider) {
        super(vilkårResultatRepository, vilkårTjeneste, behandlingRepository, vilkårsPerioderTilVurderingTjeneste, vilkårPeriodeFilterProvider);
        this.manuelleVilkårRekkefølgeTjeneste = manuelleVilkårRekkefølgeTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public VilkårType getAktuellVilkårType() {
        return VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR;
    }

    @Override
    public Set<VilkårType> getVilkårAvhengigheter(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        EnumSet<VilkårType> avhengigheter = EnumSet.noneOf(VilkårType.class);
        avhengigheter.add(VilkårType.ALDERSVILKÅR);
        avhengigheter.add(VilkårType.SØKNADSFRIST);
        avhengigheter.addAll(manuelleVilkårRekkefølgeTjeneste.finnManuelleVilkårSomErFør(getAktuellVilkårType(), ytelseType, behandlingType));
        return avhengigheter;
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
