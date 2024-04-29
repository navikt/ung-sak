package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.vilkår;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;

import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@BehandlingTypeRef
@ApplicationScoped
public class PPNVilkårsPerioderTilVurderingTjeneste extends PleiepengerVilkårsPerioderTilVurderingTjeneste {

    // MERK: DERSOM PPN og PSB begynner å divergere, må det vurderes å erstatte arv med komposisjon

    PPNVilkårsPerioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public PPNVilkårsPerioderTilVurderingTjeneste(@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE) VilkårUtleder vilkårUtleder,
                                                  VilkårResultatRepository vilkårResultatRepository,
                                                  BehandlingRepository behandlingRepository,
                                                  PleietrengendeRevurderingPerioderTjeneste pleietrengendeRevurderingPerioderTjeneste,
                                                  RevurderingPerioderTjeneste revurderingPerioderTjeneste,
                                                  SøknadsperiodeTjeneste søknadsperiodeTjeneste,
                                                  UtsattBehandlingAvPeriodeRepository utsattBehandlingAvPeriodeRepository,
                                                  @FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE) PleiepengerEndringIUttakPeriodeUtleder endringIUttakPeriodeUtleder) {
        super(vilkårUtleder
            , Map.of()
            , vilkårResultatRepository
            , behandlingRepository
            , revurderingPerioderTjeneste
            , pleietrengendeRevurderingPerioderTjeneste
            , søknadsperiodeTjeneste
            , utsattBehandlingAvPeriodeRepository,
            endringIUttakPeriodeUtleder);
    }

    @Override
    public Set<VilkårType> definerendeVilkår() {
        return Set.of(VilkårType.I_LIVETS_SLUTTFASE);
    }

}
