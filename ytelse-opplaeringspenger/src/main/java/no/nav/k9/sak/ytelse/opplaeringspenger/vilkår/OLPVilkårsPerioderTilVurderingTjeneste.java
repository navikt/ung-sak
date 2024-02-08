package no.nav.k9.sak.ytelse.opplaeringspenger.vilkår;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.inngangsvilkår.VilkårUtleder;
import no.nav.k9.sak.utsatt.UtsattBehandlingAvPeriodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleiepengerEndringIUttakPeriodeUtleder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.PleiepengerVilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering.PleietrengendeRevurderingPerioderTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.revurdering.RevurderingPerioderTjeneste;

@ApplicationScoped
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
public class OLPVilkårsPerioderTilVurderingTjeneste extends PleiepengerVilkårsPerioderTilVurderingTjeneste {

    // MERK: DERSOM PPN og PSB begynner å divergere, må det vurderes å erstatte arv med komposisjon

    OLPVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public OLPVilkårsPerioderTilVurderingTjeneste(@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER) VilkårUtleder vilkårUtleder,
                                                  VilkårResultatRepository vilkårResultatRepository,
                                                  BehandlingRepository behandlingRepository,
                                                  PleietrengendeRevurderingPerioderTjeneste pleietrengendeRevurderingPerioderTjeneste,
                                                  RevurderingPerioderTjeneste revurderingPerioderTjeneste,
                                                  SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                  @FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER) PleiepengerEndringIUttakPeriodeUtleder endringIUttakPeriodeUtleder,
                                                  UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository) {

        super(vilkårUtleder
            , Map.of()
            , vilkårResultatRepository
            , behandlingRepository
            ,
            revurderingPerioderTjeneste
            , pleietrengendeRevurderingPerioderTjeneste
            , søknadsperiodeTjeneste
            , utsattBehandlingAvPeriodeRepository,
            endringIUttakPeriodeUtleder);
    }

    @Override
    public Set<VilkårType> definerendeVilkår() {
        Set<VilkårType> vilkårIRekkefølge = new LinkedHashSet<>();
        vilkårIRekkefølge.add(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON);
        vilkårIRekkefølge.add(VilkårType.LANGVARIG_SYKDOM);
        vilkårIRekkefølge.add(VilkårType.GJENNOMGÅ_OPPLÆRING);
        vilkårIRekkefølge.add(VilkårType.NØDVENDIG_OPPLÆRING);
        return vilkårIRekkefølge;
    }
}
