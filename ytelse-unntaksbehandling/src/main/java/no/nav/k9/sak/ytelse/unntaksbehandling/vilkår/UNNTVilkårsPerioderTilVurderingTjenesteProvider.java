package no.nav.k9.sak.ytelse.unntaksbehandling.vilkår;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
public class UNNTVilkårsPerioderTilVurderingTjenesteProvider {

    private static final String YTELSE_OMS = "OMP";
    private static final String YTELSE_PLEIEPENGER = "PSB";
    private static final String YTELSE_FRISINN = "FRISINN";

    private UNNTVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;

    UNNTVilkårsPerioderTilVurderingTjenesteProvider() {
        // CDI
    }

    @Inject
    UNNTVilkårsPerioderTilVurderingTjenesteProvider(UNNTVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste) {
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
    }

    @FagsakYtelseTypeRef(YTELSE_OMS)
    @BehandlingTypeRef("BT-010")
    @Produces
    @ApplicationScoped
    public VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjenesteOMS() {
        return vilkårsPerioderTilVurderingTjeneste;
    }

    @FagsakYtelseTypeRef(YTELSE_PLEIEPENGER)
    @BehandlingTypeRef("BT-010")
    @Produces
    @ApplicationScoped
    public VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjenestePSB() {
        return vilkårsPerioderTilVurderingTjeneste;
    }

    @FagsakYtelseTypeRef(YTELSE_FRISINN)
    @BehandlingTypeRef("BT-010")
    @Produces
    @ApplicationScoped
    public VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjenesteFRI() {
        return vilkårsPerioderTilVurderingTjeneste;
    }

}
