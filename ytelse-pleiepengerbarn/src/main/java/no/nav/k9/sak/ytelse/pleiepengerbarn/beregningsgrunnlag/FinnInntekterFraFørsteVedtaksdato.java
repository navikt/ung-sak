package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.FinnInntekterFraForrigeRegisterinnhenting;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.SigruninntekterForBeregningFilter;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(FagsakYtelseType.OMSORGSPENGER)
class FinnInntekterFraFørsteVedtaksdato implements SigruninntekterForBeregningFilter {

    private BehandlingRepository behandlingRepository;
    private Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;

    public FinnInntekterFraFørsteVedtaksdato() {
    }

    @Inject
    public FinnInntekterFraFørsteVedtaksdato(BehandlingRepository behandlingRepository,
                                             @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                             InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                             @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
    }


    @Override
    public List<Inntekt> finnInntekter(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate skjæringstidspunkt) {
        var forrigeBehandlingMedStpTilVurderingOpt = finnForrigeBehandlingMedSkjæringstidspunktTilVurdering(behandlingReferanse, skjæringstidspunkt);
        if (forrigeBehandlingMedStpTilVurderingOpt.isPresent()) {
            var forrigeBehandlingRef = forrigeBehandlingMedStpTilVurderingOpt.get();
            var forrigeIayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(forrigeBehandlingRef.getBehandlingId());
            if (skalSjekkeForrigeBehandling(behandlingReferanse, iayGrunnlag, forrigeBehandlingRef, forrigeIayGrunnlag, skjæringstidspunkt)) {
                return finnInntekter(forrigeBehandlingRef, forrigeIayGrunnlag, skjæringstidspunkt);
            }
        }
        return new FinnInntekterFraForrigeRegisterinnhenting().finnInntekter(behandlingReferanse, iayGrunnlag, skjæringstidspunkt);
    }

    private boolean skalSjekkeForrigeBehandling(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag iayGrunnlag, BehandlingReferanse forrigeBehandlingRef, InntektArbeidYtelseGrunnlag forrigeIayGrunnlag, LocalDate skjæringstidspunkt) {
        return erNæringVedStp(behandlingReferanse, iayGrunnlag, skjæringstidspunkt) &&
            erNæringVedStp(forrigeBehandlingRef, forrigeIayGrunnlag, skjæringstidspunkt) &&
            !erKlage(behandlingReferanse);
    }

    private Optional<BehandlingReferanse> finnForrigeBehandlingMedSkjæringstidspunktTilVurdering(BehandlingReferanse behandlingReferanse, LocalDate skjæringstidspunkt) {
        boolean harSammeStpIGjeldendeBehandling = false;
        var gjeldendeBehandling = Optional.of(behandlingReferanse);
        while (!harSammeStpIGjeldendeBehandling) {
            gjeldendeBehandling = finnOriginalBehandlingReferanse(gjeldendeBehandling.get());
            if (gjeldendeBehandling.isEmpty()) {
                return Optional.empty();
            }
            harSammeStpIGjeldendeBehandling = vurderesPeriodeIBehandling(skjæringstidspunkt, gjeldendeBehandling.get());
        }
        return gjeldendeBehandling;
    }

    private boolean vurderesPeriodeIBehandling(LocalDate skjæringstidspunkt, BehandlingReferanse gjeldendeBehandling) {
        var perioderTilVurderingTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(this.vilkårsPerioderTilVurderingTjeneste,
            gjeldendeBehandling.getFagsakYtelseType(), gjeldendeBehandling.getBehandlingType());
        return perioderTilVurderingTjeneste.utled(gjeldendeBehandling.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR).stream()
            .anyMatch(p -> p.getFomDato().equals(skjæringstidspunkt));
    }

    private Optional<BehandlingReferanse> finnOriginalBehandlingReferanse(BehandlingReferanse behandlingReferanse) {
        if (behandlingReferanse.getOriginalBehandlingId().isEmpty()) {
            return Optional.empty();
        }
        var originalBehandlingId = behandlingReferanse.getOriginalBehandlingId().get();
        var originalBehandling = behandlingRepository.hentBehandling(originalBehandlingId);
        return Optional.of(BehandlingReferanse.fra(originalBehandling));
    }

    private boolean erNæringVedStp(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate skjæringstidspunkt) {
        var oppgittOpptjening = finnOpptjeningForBeregningTjeneste(behandlingReferanse).finnOppgittOpptjening(behandlingReferanse, iayGrunnlag, skjæringstidspunkt);
        return oppgittOpptjening.stream().flatMap(o -> o.getEgenNæring().stream()).anyMatch(e -> e.getPeriode().inkluderer(skjæringstidspunkt.minusDays(1)));
    }

    private boolean erKlage(BehandlingReferanse behandlingReferanse) {
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.getBehandlingId());
        return behandling.getBehandlingÅrsakerTyper().stream().anyMatch(a -> a.equals(BehandlingÅrsakType.ETTER_KLAGE_INNHENT_SIGRUN)); // burde vi hatt en egen årsak for reinnhenting av sigruninntekter?
    }

    private OpptjeningForBeregningTjeneste finnOpptjeningForBeregningTjeneste(BehandlingReferanse referanse) {
        FagsakYtelseType ytelseType = referanse.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(opptjeningForBeregningTjeneste, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpptjeningForBeregningTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }


}
