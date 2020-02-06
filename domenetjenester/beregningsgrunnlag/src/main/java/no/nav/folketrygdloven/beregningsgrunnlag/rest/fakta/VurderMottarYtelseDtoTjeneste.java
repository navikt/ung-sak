package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.ArbeidstakerUtenInntektsmeldingTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.VurderMottarYtelseTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.ArbeidstakerUtenInntektsmeldingAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FaktaOmBeregningDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.VurderMottarYtelseDto;
import no.nav.k9.sak.typer.AktørId;

@ApplicationScoped
public class VurderMottarYtelseDtoTjeneste implements FaktaOmBeregningTilfelleDtoTjeneste {

    private BeregningsgrunnlagDtoUtil beregningsgrunnlagDtoUtil;
    private FaktaOmBeregningAndelDtoTjeneste faktaOmBeregningAndelDtoTjeneste;

    public VurderMottarYtelseDtoTjeneste() {
        // For CDI
    }

    @Inject
    public VurderMottarYtelseDtoTjeneste(BeregningsgrunnlagDtoUtil beregningsgrunnlagDtoUtil,
                                         FaktaOmBeregningAndelDtoTjeneste faktaOmBeregningAndelDtoTjeneste) {
        this.beregningsgrunnlagDtoUtil = beregningsgrunnlagDtoUtil;
        this.faktaOmBeregningAndelDtoTjeneste = faktaOmBeregningAndelDtoTjeneste;
    }

    @Override
    public void lagDto(BeregningsgrunnlagInput input, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlagOpt, FaktaOmBeregningDto faktaOmBeregningDto) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = input.getBeregningsgrunnlag();
        Optional<BeregningsgrunnlagEntitet> forrigeBeregningsgrunnlagOpt = forrigeGrunnlagOpt
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        if (beregningsgrunnlag.getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE)) {
            LocalDate skjæringstidspunkt = beregningsgrunnlag.getSkjæringstidspunkt();
            var ref = input.getBehandlingReferanse();
            var iayGrunnlag = input.getIayGrunnlag();
            AktørId aktørId = ref.getAktørId();
            if (forrigeBeregningsgrunnlagOpt.isPresent() &&
                forrigeBeregningsgrunnlagOpt.get().getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.VURDER_MOTTAR_YTELSE)) {
                BeregningsgrunnlagEntitet bg = finnGjelendeBeregningsgrunnlag(beregningsgrunnlag, forrigeBeregningsgrunnlagOpt.get());
                byggVerdier(aktørId, iayGrunnlag, bg, faktaOmBeregningDto, skjæringstidspunkt);
            } else {
                byggVerdier(aktørId, iayGrunnlag, beregningsgrunnlag, faktaOmBeregningDto, skjæringstidspunkt);
            }
        }
    }

    private BeregningsgrunnlagEntitet finnGjelendeBeregningsgrunnlag(BeregningsgrunnlagEntitet oppretteBg, BeregningsgrunnlagEntitet forrigeBg) {
        List<BeregningsgrunnlagPeriode> opprettetPerioder = oppretteBg.getBeregningsgrunnlagPerioder();
        List<BeregningsgrunnlagPeriode> forrigePerioder = forrigeBg.getBeregningsgrunnlagPerioder();
        if (antallPerioderOgAndelerMatcher(opprettetPerioder, forrigePerioder)) {
            return forrigeBg;
        }
        return oppretteBg;
    }

    private boolean antallPerioderOgAndelerMatcher(List<BeregningsgrunnlagPeriode> opprettetPerioder, List<BeregningsgrunnlagPeriode> forrigePerioder) {
        if (opprettetPerioder.size() != forrigePerioder.size()) {
            return false;
        }
        return !erAndelerIPeriodeForskjelligeStørrelse(opprettetPerioder, forrigePerioder);
    }

    private boolean erAndelerIPeriodeForskjelligeStørrelse(List<BeregningsgrunnlagPeriode> opprettetPerioder, List<BeregningsgrunnlagPeriode> forrigePerioder) {
        for (int i = 0; i < opprettetPerioder.size(); i++) {
            List<BeregningsgrunnlagPrStatusOgAndel> opprettetAndeler = opprettetPerioder.get(i).getBeregningsgrunnlagPrStatusOgAndelList();
            List<BeregningsgrunnlagPrStatusOgAndel> forrigeAndeler = forrigePerioder.get(i).getBeregningsgrunnlagPrStatusOgAndelList();
            if (opprettetAndeler.size() != forrigeAndeler.size()) {
                return true;
            }
        }
        return false;
    }

    private void byggVerdier(AktørId aktørId, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag, BeregningsgrunnlagEntitet beregningsgrunnlag,
                             FaktaOmBeregningDto faktaOmBeregningDto, LocalDate skjæringstidspunkt) {
        VurderMottarYtelseDto vurderMottarYtelseDto = new VurderMottarYtelseDto();
        if (VurderMottarYtelseTjeneste.erFrilanser(beregningsgrunnlag)) {
            lagFrilansDel(aktørId, beregningsgrunnlag, inntektArbeidYtelseGrunnlag, vurderMottarYtelseDto, skjæringstidspunkt);
            if (faktaOmBeregningDto.getFrilansAndel() == null) {
                faktaOmBeregningAndelDtoTjeneste.lagFrilansAndelDto(beregningsgrunnlag, inntektArbeidYtelseGrunnlag).ifPresent(faktaOmBeregningDto::setFrilansAndel);
            }
        }
        lagArbeidstakerUtenInntektsmeldingDel(inntektArbeidYtelseGrunnlag, aktørId, beregningsgrunnlag,
            vurderMottarYtelseDto, skjæringstidspunkt);
        faktaOmBeregningDto.setVurderMottarYtelse(vurderMottarYtelseDto);
    }

    private void lagArbeidstakerUtenInntektsmeldingDel(InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag,
                                                       AktørId aktørId,
                                                       BeregningsgrunnlagEntitet beregningsgrunnlag, VurderMottarYtelseDto vurderMottarYtelseDto,
                                                       LocalDate skjæringstidspunkt) {

        var filter = new InntektFilter(inntektArbeidYtelseGrunnlag.getAktørInntektFraRegister(aktørId)).før(skjæringstidspunkt);
        var andeler = ArbeidstakerUtenInntektsmeldingTjeneste.finnArbeidstakerAndelerUtenInntektsmelding(beregningsgrunnlag, inntektArbeidYtelseGrunnlag);
        andeler.forEach(andelUtenIM -> {
            var dto = new ArbeidstakerUtenInntektsmeldingAndelDto();
            beregnOgSettInntektPrMnd(filter, andelUtenIM, dto);
            dto.setAndelsnr(andelUtenIM.getAndelsnr());
            dto.setInntektskategori(andelUtenIM.getInntektskategori());
            beregningsgrunnlagDtoUtil.lagArbeidsforholdDto(andelUtenIM, Optional.empty(), inntektArbeidYtelseGrunnlag).ifPresent(dto::setArbeidsforhold);
            andelUtenIM.mottarYtelse().ifPresent(dto::setMottarYtelse);
            vurderMottarYtelseDto.leggTilArbeidstakerAndelUtenInntektsmelding(dto);
        });
    }

    private void beregnOgSettInntektPrMnd(InntektFilter filter, BeregningsgrunnlagPrStatusOgAndel andel, ArbeidstakerUtenInntektsmeldingAndelDto dto) {
        BigDecimal snittIBeregningsperioden = InntektForAndelTjeneste.finnSnittinntektForArbeidstakerIBeregningsperioden(filter, andel);
        dto.setInntektPrMnd(snittIBeregningsperioden);
    }

    private void lagFrilansDel(AktørId aktørId,
                               BeregningsgrunnlagEntitet beregningsgrunnlag,
                               InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag,
                               VurderMottarYtelseDto vurderMottarYtelseDto,
                               LocalDate skjæringstidspunkt) {
        vurderMottarYtelseDto.setErFrilans(VurderMottarYtelseTjeneste.erFrilanser(beregningsgrunnlag));
        beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(andel -> andel.getAktivitetStatus().erFrilanser()).findFirst()
            .ifPresent(frilansAndel -> {
                vurderMottarYtelseDto.setFrilansMottarYtelse(frilansAndel.mottarYtelse().orElse(null));
                InntektForAndelTjeneste.finnSnittAvFrilansinntektIBeregningsperioden(aktørId, inntektArbeidYtelseGrunnlag, frilansAndel, skjæringstidspunkt)
                    .ifPresent(vurderMottarYtelseDto::setFrilansInntektPrMnd);
            });
    }

}
