package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.UtledBeregningSkjæringstidspunktForBehandlingTjeneste;
import no.nav.k9.sak.inngangsvilkår.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PsbUtledBeregningSkjæringstidspunktForBehandlingTjeneste implements UtledBeregningSkjæringstidspunktForBehandlingTjeneste {

    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;

    @Inject
    public PsbUtledBeregningSkjæringstidspunktForBehandlingTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                                                    @FagsakYtelseTypeRef("PSB") VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
    }

    /**
     * Utleder første opptjeningsperiode som er til vurdering i denne behandlingen og som oppfyller vilkåret
     *
     * @param referanse behandlingen
     * @return skjæringstidspunkt (dato)
     */
    @Override
    public LocalDate utled(BehandlingReferanse referanse) {
        var vilkårsperioder = vilkårsPerioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.OPPTJENINGSVILKÅRET);
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());
        var opptjeningsvilkåret = vilkårene.getVilkår(VilkårType.OPPTJENINGSVILKÅRET).orElseThrow();

        return opptjeningsvilkåret.getPerioder()
            .stream()
            .filter(it -> vilkårsperioder.contains(it.getPeriode()))
            .filter(it -> Utfall.OPPFYLT.equals(it.getGjeldendeUtfall()))
            .map(VilkårPeriode::getFom)
            .min(LocalDate::compareTo)
            .orElseThrow();
    }
}
