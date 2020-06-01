package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@Dependent
public class BeregningsgrunnlagVilkårTjeneste extends BeregningsgrunnlagVilkårTjenesteFelles {



    protected BeregningsgrunnlagVilkårTjeneste() {
        // CDI Proxy
    }

    @Inject
    public BeregningsgrunnlagVilkårTjeneste(BehandlingRepository behandlingRepository,
                                            VedtakVarselRepository behandlingsresultatRepository,
                                            @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                            VilkårResultatRepository vilkårResultatRepository) {
        super(behandlingRepository, behandlingsresultatRepository, perioderTilVurderingTjenester, vilkårResultatRepository);
    }


}
