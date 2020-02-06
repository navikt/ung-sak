package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.AndelMedBeløpDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FaktaOmBeregningDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.KunYtelseDto;

@ApplicationScoped
public class KunYtelseDtoTjeneste implements FaktaOmBeregningTilfelleDtoTjeneste {

    private BeregningsgrunnlagDtoUtil dtoUtil;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    KunYtelseDtoTjeneste() {
        // For CDI
    }

    @Inject
    public KunYtelseDtoTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                BeregningsgrunnlagDtoUtil dtoUtil) {
        this.dtoUtil = dtoUtil;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    @Override
    public void lagDto(BeregningsgrunnlagInput input,
                       Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlagOpt,
                       FaktaOmBeregningDto faktaOmBeregningDto) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = input.getBeregningsgrunnlag();
        if (beregningsgrunnlag.getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.FASTSETT_BG_KUN_YTELSE)) {
            faktaOmBeregningDto.setKunYtelse(lagKunYtelseDto(input));
        }
    }

    KunYtelseDto lagKunYtelseDto(BeregningsgrunnlagInput input) {
        var ref = input.getBehandlingReferanse();
        KunYtelseDto dto = new KunYtelseDto();
        Optional<BeregningsgrunnlagEntitet> forrigeBgOpt = beregningsgrunnlagRepository
            .hentSisteBeregningsgrunnlagGrunnlagEntitet(ref.getBehandlingId(), BeregningsgrunnlagTilstand.KOFAKBER_UT)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        forrigeBgOpt
            .filter(this::harKunYtelseSomAktivitetstatus)
            .ifPresentOrElse(settVerdierFraForrige(dto, input.getIayGrunnlag()), () -> settVerdier(dto, input.getBeregningsgrunnlag(), input.getIayGrunnlag()));
        return dto;
    }

    private boolean harKunYtelseSomAktivitetstatus(BeregningsgrunnlagEntitet bg) {
        return bg.getAktivitetStatuser().stream()
            .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus)
            .collect(Collectors.toList())
            .equals(List.of(AktivitetStatus.KUN_YTELSE));
    }

    private Consumer<BeregningsgrunnlagEntitet> settVerdierFraForrige(KunYtelseDto dto, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        return forrigeBg -> {
            settVerdier(dto, forrigeBg, inntektArbeidYtelseGrunnlag);
        };
    }

    private void settVerdier(KunYtelseDto kunYtelseDto, BeregningsgrunnlagEntitet beregningsgrunnlag, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        BeregningsgrunnlagPeriode periode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        periode.getBeregningsgrunnlagPrStatusOgAndelList().forEach(andel -> {
            var dto = new AndelMedBeløpDto();
            dto.setAndelsnr(andel.getAndelsnr());
            dto.setLagtTilAvSaksbehandler(andel.getLagtTilAvSaksbehandler());
            dto.setFastsattAvSaksbehandler(Boolean.TRUE.equals(andel.getFastsattAvSaksbehandler()));
            dto.setAktivitetStatus(andel.getAktivitetStatus());
            dto.setInntektskategori(andel.getInntektskategori());
            dto.setFastsattBelopPrMnd(finnFastsattMånedsbeløp(andel));
            dtoUtil.lagArbeidsforholdDto(andel, Optional.empty(), inntektArbeidYtelseGrunnlag)
                .ifPresent(dto::setArbeidsforhold);
            kunYtelseDto.leggTilAndel(dto);
        });
    }

    private BigDecimal finnFastsattMånedsbeløp(BeregningsgrunnlagPrStatusOgAndel andel) {
        return andel.getBeregnetPrÅr() != null ? andel.getBeregnetPrÅr().divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP) : null;
    }
}
