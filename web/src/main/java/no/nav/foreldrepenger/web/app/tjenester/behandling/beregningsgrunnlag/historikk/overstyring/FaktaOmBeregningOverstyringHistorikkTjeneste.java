package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.overstyring;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.MatchBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettBeregningsgrunnlagAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller.Lønnsendring;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.InntektHistorikkTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.MapTilLønnsendring;

@ApplicationScoped
public class FaktaOmBeregningOverstyringHistorikkTjeneste {

    private InntektHistorikkTjeneste inntektHistorikkTjeneste;

    public FaktaOmBeregningOverstyringHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public FaktaOmBeregningOverstyringHistorikkTjeneste(InntektHistorikkTjeneste inntektHistorikkTjeneste) {
        this.inntektHistorikkTjeneste = inntektHistorikkTjeneste;
    }

    /**
     *  Lager historikk for overstyring av inntekter, refusjon og inntektskategori i fakta om beregning.
     *  @param behandlingId Id for behandling
     * @param dto Dto for overstyring av beregningsgrunnlag
     * @param tekstBuilder Builder for historikkinnslag
     * @param nyttBeregningsgrunnlag Aktivt og oppdatert beregningsgrunnlag
     * @param forrigeGrunnlag Forrige beregningsgrunnlag fra KOFAKBER_UT
     * @param iayGrunnlag InntektArbeidYtelseGrunnlag
     */
    public void lagHistorikk(Long behandlingId,
                             OverstyrBeregningsgrunnlagDto dto,
                             HistorikkInnslagTekstBuilder tekstBuilder,
                             BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        List<BeregningsgrunnlagPeriode> bgPerioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        List<FastsettBeregningsgrunnlagAndelDto> overstyrteAndeler = dto.getOverstyrteAndeler();
        for (BeregningsgrunnlagPeriode bgPeriode : bgPerioder) {
            Optional<BeregningsgrunnlagPeriode> forrigeBgPeriode = MatchBeregningsgrunnlagTjeneste
                .finnOverlappendePeriodeOmKunEnFinnes(bgPeriode, forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
            List<Lønnsendring> endringer = overstyrteAndeler.stream()
                .map(andelDto -> MapTilLønnsendring.mapTilLønnsendringForAndelIPeriode(andelDto, andelDto.getFastsatteVerdier(), bgPeriode, forrigeBgPeriode))
                .collect(Collectors.toList());
            inntektHistorikkTjeneste.lagHistorikk(tekstBuilder, endringer, iayGrunnlag);
        }
        settSkjermlenke(tekstBuilder);
        tekstBuilder.ferdigstillHistorikkinnslagDel();
    }

    private void settSkjermlenke(HistorikkInnslagTekstBuilder tekstBuilder) {
        boolean erSkjermlenkeSatt = tekstBuilder.getHistorikkinnslagDeler().stream().anyMatch(historikkDel -> historikkDel.getSkjermlenke().isPresent());
        if (!erSkjermlenkeSatt) {
            tekstBuilder.medSkjermlenke(SkjermlenkeType.FAKTA_OM_BEREGNING);
        }
    }
}
