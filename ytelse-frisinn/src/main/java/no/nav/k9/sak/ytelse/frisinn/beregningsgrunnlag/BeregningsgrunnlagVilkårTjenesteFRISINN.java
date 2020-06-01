package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjenesteFelles;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.frisinn.vilkår.IkkeKantIKant;

@Dependent
public class BeregningsgrunnlagVilkårTjenesteFRISINN extends BeregningsgrunnlagVilkårTjenesteFelles {


    protected BeregningsgrunnlagVilkårTjenesteFRISINN() {
        // CDI Proxy
    }

    @Inject
    public BeregningsgrunnlagVilkårTjenesteFRISINN(BehandlingRepository behandlingRepository,
                                                   VedtakVarselRepository behandlingsresultatRepository,
                                                   @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester,
                                                   VilkårResultatRepository vilkårResultatRepository) {
        super(behandlingRepository, behandlingsresultatRepository, perioderTilVurderingTjenester, vilkårResultatRepository);
    }

    @Override
    protected VilkårBuilder hentBuilderForVilkår(VilkårResultatBuilder builder) {
        return builder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).medKantIKantVurderer(new IkkeKantIKant());
    }

    @Override
    protected VilkårResultatBuilder hentVilkårResultatBuilderFraEksisterende(Vilkårene vilkårene) {
        return Vilkårene.builderFraEksisterende(vilkårene).medKantIKantVurderer(new IkkeKantIKant());
    }


}
