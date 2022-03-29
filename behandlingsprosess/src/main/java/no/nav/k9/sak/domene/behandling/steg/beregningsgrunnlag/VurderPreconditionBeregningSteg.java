package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.PRECONDITION_BEREGNING;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@FagsakYtelseTypeRef
@BehandlingStegRef(stegtype = PRECONDITION_BEREGNING)
@BehandlingTypeRef
@ApplicationScoped
public class VurderPreconditionBeregningSteg implements BeregningsgrunnlagSteg {

    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;
    private Instance<PreconditionBeregningAksjonspunktUtleder> aksjonspunktUtledere;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BeregningsgrunnlagTjeneste kalkulusTjeneste;


    protected VurderPreconditionBeregningSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderPreconditionBeregningSteg(VilkårResultatRepository vilkårResultatRepository,
                                           BehandlingRepository behandlingRepository,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                           @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste,
                                           @Any Instance<PreconditionBeregningAksjonspunktUtleder> aksjonspunktUtledere,
                                           BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                           BeregningsgrunnlagTjeneste kalkulusTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.aksjonspunktUtledere = aksjonspunktUtledere;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.kalkulusTjeneste = kalkulusTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var referanse = BehandlingReferanse.fra(behandling);

        // Setter perioder til vurdering, deaktiverer grunnlag for referanser som er inaktive (utdaterte skjæringstidspunkt)
        deaktiverResultatOgSettPeriodeTilVurdering(referanse, kontekst);

        // Avslår dersom vi ikke har grunnlag for å kalle kalkulus (mangler data eller har avslag)
        avslåBeregningVedBehov(kontekst, behandling, referanse);

        return BehandleStegResultat.utførtMedAksjonspunktResultater(finnAksjonspunkter(behandling));
    }

    private void avslåBeregningVedBehov(BehandlingskontrollKontekst kontekst, Behandling behandling, BehandlingReferanse referanse) {
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        var vilkåret = vilkårene.getVilkår(VilkårType.OPPTJENINGSVILKÅRET)
            .orElseThrow();
        var vurdertePerioder = vurdertePerioder(VilkårType.OPPTJENINGSVILKÅRET, referanse);
        var opptjeningForBeregningTjeneste = finnOpptjeningForBeregningTjeneste(behandling);
        var grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        var avslåttePerioder = vilkåret.getPerioder()
            .stream()
            .filter(it -> vurdertePerioder.contains(it.getPeriode()))
            .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getGjeldendeUtfall()) || ingenBeregningsAktiviteter(opptjeningForBeregningTjeneste, it, grunnlag, referanse))
            .collect(Collectors.toList());

        var noeAvslått = !vilkåret.getPerioder().isEmpty() && !avslåttePerioder.isEmpty();

        if (noeAvslått) {
            avslåBerregningsperioderDerHvorOpptjeningErAvslått(referanse, vilkårene, avslåttePerioder);
        }
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

    public Set<DatoIntervallEntitet> vurdertePerioder(VilkårType vilkårType, BehandlingReferanse ref) {
        var tjeneste = getPerioderTilVurderingTjeneste(ref);
        return tjeneste.utled(ref.getId(), vilkårType);
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjeneste, ref.getFagsakYtelseType(), ref.getBehandlingType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + ref.getFagsakYtelseType() + "], behandlingtype [" + ref.getBehandlingType() + "]"));
    }

    private OpptjeningForBeregningTjeneste finnOpptjeningForBeregningTjeneste(Behandling behandling) {
        FagsakYtelseType ytelseType = behandling.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(opptjeningForBeregningTjeneste, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpptjeningForBeregningTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }

    private List<AksjonspunktResultat> finnAksjonspunkter(Behandling behandling) {
        FagsakYtelseType ytelseType = behandling.getFagsakYtelseType();
        var tjeneste = FagsakYtelseTypeRef.Lookup.find(aksjonspunktUtledere, ytelseType);
        return tjeneste.map(utleder -> utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling))))
            .orElse(Collections.emptyList());
    }


    private void deaktiverResultatOgSettPeriodeTilVurdering(BehandlingReferanse ref, BehandlingskontrollKontekst kontekst) {
        ryddVedtaksresultatForPerioderTilVurdering(kontekst, ref);
        kalkulusTjeneste.deaktiverBeregningsgrunnlagUtenTilknytningTilVilkår(ref);
    }

    private void ryddVedtaksresultatForPerioderTilVurdering(BehandlingskontrollKontekst kontekst, BehandlingReferanse ref) {
        var tjeneste = getPerioderTilVurderingTjeneste(ref);
        var perioderTilVurdering = tjeneste.utled(ref.getId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst, perioderTilVurdering);
    }


}
