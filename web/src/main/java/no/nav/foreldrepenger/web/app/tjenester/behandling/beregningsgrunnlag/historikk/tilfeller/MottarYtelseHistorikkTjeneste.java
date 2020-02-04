package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.tilfeller;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.ArbeidstakerandelUtenIMMottarYtelseDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.MottarYtelseDto;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.ArbeidstakerUtenInntektsmeldingTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.VurderMottarYtelseTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_MOTTAR_YTELSE")
public class MottarYtelseHistorikkTjeneste extends FaktaOmBeregningHistorikkTjeneste {

    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste;

    public MottarYtelseHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public MottarYtelseHistorikkTjeneste(ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslagTjeneste) {
        this.arbeidsgiverHistorikkinnslagTjeneste = arbeidsgiverHistorikkinnslagTjeneste;
    }

    @Override
    public void lagHistorikk(Long behandlingId, FaktaBeregningLagreDto dto, HistorikkInnslagTekstBuilder tekstBuilder, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
            MottarYtelseDto mottarYtelseDto = dto.getMottarYtelse();
        Optional<BeregningsgrunnlagEntitet> forrigeBG = forrigeGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        if (VurderMottarYtelseTjeneste.erFrilanser(nyttBeregningsgrunnlag) && mottarYtelseDto.getFrilansMottarYtelse() != null) {
                lagHistorikkinnslagForFrilans(forrigeBG, mottarYtelseDto, tekstBuilder);
        }
        List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer = iayGrunnlag.getArbeidsforholdOverstyringer();
        ArbeidstakerUtenInntektsmeldingTjeneste.finnArbeidstakerAndelerUtenInntektsmelding(nyttBeregningsgrunnlag, iayGrunnlag)
            .forEach(andel -> {
                Optional<Boolean> mottarYtelseVerdi = mottarYtelseDto.getArbeidstakerUtenIMMottarYtelse().stream()
                    .filter(mottarYtelseAndel -> mottarYtelseAndel.getAndelsnr() == andel.getAndelsnr())
                    .findFirst().map(ArbeidstakerandelUtenIMMottarYtelseDto::getMottarYtelse);
                lagHistorikkinnslagForArbeidstakerUtenIM(forrigeBG, andel, mottarYtelseVerdi, tekstBuilder, arbeidsforholdOverstyringer);
            });
    }

    private void lagHistorikkinnslagForArbeidstakerUtenIM(Optional<BeregningsgrunnlagEntitet> forrigeBG, BeregningsgrunnlagPrStatusOgAndel andel,
                                                          Optional<Boolean> mottarYtelseVerdi, HistorikkInnslagTekstBuilder tekstBuilder,
                                                          List<ArbeidsforholdOverstyring> arbeidsforholdOverstyringer) {
        mottarYtelseVerdi.ifPresent(mottarYtelse -> {
                Optional<Boolean> mottarYtelseForrige = finnVerdiForMottarYtelseForAndelIForrigeGrunnlag(andel, forrigeBG);
                if (!mottarYtelseForrige.isPresent() || !mottarYtelseForrige.get().equals(mottarYtelse)){
                    String andelsInfo = arbeidsgiverHistorikkinnslagTjeneste.lagHistorikkinnslagTekstForBeregningsgrunnlag(andel.getAktivitetStatus(),
                        andel.getArbeidsgiver(),
                        andel.getArbeidsforholdRef(),
                        arbeidsforholdOverstyringer);
                    tekstBuilder
                        .medEndretFelt(HistorikkEndretFeltType.MOTTAR_YTELSE_ARBEID, andelsInfo,
                            mottarYtelseForrige.orElse(null), mottarYtelse);
                }
            }
        );
    }

    private void lagHistorikkinnslagForFrilans(Optional<BeregningsgrunnlagEntitet> forrigeBG, MottarYtelseDto mottarYtelseDto,
                                               HistorikkInnslagTekstBuilder tekstBuilder) {
        Optional<Boolean> mottarYtelseForrige = finnVerdiForMottarYtelseForFrilansIForrigeGrunnlag(forrigeBG);
        if (!mottarYtelseForrige.isPresent() || !mottarYtelseForrige.get().equals(mottarYtelseDto.getFrilansMottarYtelse())) {
            tekstBuilder
                .medEndretFelt(HistorikkEndretFeltType.MOTTAR_YTELSE_FRILANS, mottarYtelseForrige.orElse(null), mottarYtelseDto.getFrilansMottarYtelse());
        }
    }

    private Optional<Boolean> finnVerdiForMottarYtelseForFrilansIForrigeGrunnlag(Optional<BeregningsgrunnlagEntitet> forrigeBG) {
        return forrigeBG
            .stream().flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.getAktivitetStatus().erFrilanser())
            .findFirst().stream()
            .flatMap(andel -> andel.mottarYtelse().stream()).findFirst();
    }

    private Optional<Boolean> finnVerdiForMottarYtelseForAndelIForrigeGrunnlag(BeregningsgrunnlagPrStatusOgAndel andelINyttBg, Optional<BeregningsgrunnlagEntitet> forrigeBG) {
        return forrigeBG
            .stream().flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> andel.gjelderSammeArbeidsforhold(andelINyttBg))
            .findFirst().stream()
            .flatMap(andel -> andel.mottarYtelse().stream()).findFirst();
    }

}
