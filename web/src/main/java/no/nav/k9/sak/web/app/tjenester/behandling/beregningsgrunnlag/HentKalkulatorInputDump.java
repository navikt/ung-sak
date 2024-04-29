package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelseKalkulator;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Dependent
public class HentKalkulatorInputDump {
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BeregningsgrunnlagYtelseKalkulator forvaltningBeregning;

    public HentKalkulatorInputDump() {
    }

    @Inject
    public HentKalkulatorInputDump(BeregningsgrunnlagYtelseKalkulator forvaltningBeregning,
                                   InntektArbeidYtelseTjeneste iayTjeneste,
                                   BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste) {
        this.forvaltningBeregning = forvaltningBeregning;
        this.iayTjeneste = iayTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
    }

    public List<KalkulatorInputPrVilkårperiodeDto> getKalkulatorInputPrVilkårperiodeDtos(BehandlingReferanse ref) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer());

        var mapper = forvaltningBeregning.getYtelsesspesifikkMapper(ref.getFagsakYtelseType());

        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, false);

        return perioderTilVurdering.stream()
            .filter(periode -> !periodeErUtenforFagsaksIntervall(periode, ref.getFagsakPeriode()))
            .map(vilkårsperiode -> {
                var ytelseGrunnlag = mapper.lagYtelsespesifiktGrunnlag(ref, vilkårsperiode);
                var kalkulatorInput = forvaltningBeregning.getKalkulatorInputTjeneste(ref.getFagsakYtelseType()).byggDto(ref, iayGrunnlag, sakInntektsmeldinger, ytelseGrunnlag, vilkårsperiode, null);
                return new KalkulatorInputPrVilkårperiodeDto(vilkårsperiode, kalkulatorInput);
            })
            .collect(Collectors.toList());
    }


    private boolean periodeErUtenforFagsaksIntervall(DatoIntervallEntitet vilkårPeriode, DatoIntervallEntitet fagsakPeriode) {
        return !vilkårPeriode.overlapper(fagsakPeriode);
    }


}
