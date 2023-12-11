package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;

@Dependent
public class VurderAvslagGrunnetOpptjening {

    private static final Logger log = LoggerFactory.getLogger(VurderAvslagGrunnetOpptjening.class);
    private final VilkårResultatRepository vilkårResultatRepository;
    private final InntektArbeidYtelseTjeneste iayTjeneste;
    private final Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste;
    private final Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;
    private final VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;

    @Inject
    public VurderAvslagGrunnetOpptjening(VilkårResultatRepository vilkårResultatRepository,
                                         InntektArbeidYtelseTjeneste iayTjeneste,
                                         @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                         @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste,
                                         VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.iayTjeneste = iayTjeneste;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
    }

    /**
     * Vurderer om beregning skal avslås med årsak IKKE_TILSTREKKELIG_OPPTJENING
     *
     * @param referanse Behandlingreferanse
     */
    public void vurderAvslagGrunnetAvslagIOpptjening(BehandlingReferanse referanse) {
        var periodeFilter = vilkårPeriodeFilterProvider.getFilter(referanse).ignorerForlengelseperioder();
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());
        var vilkåret = vilkårene.getVilkår(VilkårType.OPPTJENINGSVILKÅRET)
            .orElseThrow();
        var vurdertePerioderIOpptjening = vurdertePerioder(VilkårType.OPPTJENINGSVILKÅRET, referanse);

        var perioderTilVurderingIBeregning = periodeFilter.filtrerPerioder(vurdertePerioderIOpptjening, VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .stream().map(PeriodeTilVurdering::getPeriode).toList();

        var opptjeningForBeregningTjeneste = finnOpptjeningForBeregningTjeneste(referanse);
        var grunnlag = iayTjeneste.hentGrunnlag(referanse.getBehandlingId());

        var avslåttePerioder = vilkåret.getPerioder()
            .stream()
            .filter(it -> perioderTilVurderingIBeregning.contains(it.getPeriode()))
            .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getGjeldendeUtfall()) || (ikkeInnvilgetEtter847(it) && ingenBeregningsAktiviteter(opptjeningForBeregningTjeneste, it, grunnlag, referanse)))
            .collect(Collectors.toList());

        var noeAvslått = !vilkåret.getPerioder().isEmpty() && !avslåttePerioder.isEmpty();

        if (noeAvslått) {
            log.info("Avslår beregning for perioder {}", avslåttePerioder);
            avslåBerregningsperioderDerHvorOpptjeningErAvslått(referanse, vilkårene, avslåttePerioder);
        }
    }

    private boolean ikkeInnvilgetEtter847(VilkårPeriode it) {
        return !(Utfall.OPPFYLT == it.getGjeldendeUtfall() && Set.of(VilkårUtfallMerknad.VM_7847_A, VilkårUtfallMerknad.VM_7847_B).contains(it.getMerknad()));
    }

    private boolean ingenBeregningsAktiviteter(OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste, VilkårPeriode it, InntektArbeidYtelseGrunnlag grunnlag, BehandlingReferanse referanse) {
        var opptjeningAktiviteter = opptjeningForBeregningTjeneste.hentEksaktOpptjeningForBeregning(referanse, grunnlag, it.getPeriode());
        return opptjeningAktiviteter.isEmpty();
    }

    private void avslåBerregningsperioderDerHvorOpptjeningErAvslått(BehandlingReferanse referanse, Vilkårene vilkårene, List<VilkårPeriode> avslåttePerioder) {

        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        for (VilkårPeriode vilkårPeriode : avslåttePerioder) {
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(vilkårPeriode.getPeriode())
                .medUtfall(Utfall.IKKE_OPPFYLT)
                .medAvslagsårsak(Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING));
        }
        builder.leggTil(vilkårBuilder);

        vilkårResultatRepository.lagre(referanse.getBehandlingId(), builder.build());
    }

    private Set<DatoIntervallEntitet> vurdertePerioder(VilkårType vilkårType, BehandlingReferanse ref) {
        var tjeneste = getPerioderTilVurderingTjeneste(ref);
        var perioderTilVurdering = tjeneste.utled(ref.getId(), vilkårType);
        var periodeFilter = vilkårPeriodeFilterProvider.getFilter(ref).ignorerForlengelseperioder();
        return periodeFilter.filtrerPerioder(perioderTilVurdering, vilkårType).stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toSet());
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjeneste, ref.getFagsakYtelseType(), ref.getBehandlingType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + ref.getFagsakYtelseType() + "], behandlingtype [" + ref.getBehandlingType() + "]"));
    }

    private OpptjeningForBeregningTjeneste finnOpptjeningForBeregningTjeneste(BehandlingReferanse behandlingRef) {
        FagsakYtelseType ytelseType = behandlingRef.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(opptjeningForBeregningTjeneste, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpptjeningForBeregningTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }


}
