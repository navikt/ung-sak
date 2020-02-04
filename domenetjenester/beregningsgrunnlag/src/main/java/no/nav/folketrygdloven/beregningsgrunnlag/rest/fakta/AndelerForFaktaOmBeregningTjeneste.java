package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.BeregningInntektsmeldingTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.felles.BeregningsgrunnlagDiffSjekker;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FordelBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.AndelForFaktaOmBeregningDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;

@ApplicationScoped
public class AndelerForFaktaOmBeregningTjeneste {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private VisningsnavnForAktivitetTjeneste visningsnavnForAktivitetTjeneste;
    private BeregningsgrunnlagDtoUtil beregningsgrunnlagDtoUtil;
    private FordelBeregningsgrunnlagTjeneste refusjonOgGraderingTjeneste;


    @Inject
    public AndelerForFaktaOmBeregningTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                              VisningsnavnForAktivitetTjeneste visningsnavnForAktivitetTjeneste,
                                              BeregningsgrunnlagDtoUtil beregningsgrunnlagDtoUtil, FordelBeregningsgrunnlagTjeneste refusjonOgGraderingTjeneste) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.visningsnavnForAktivitetTjeneste = visningsnavnForAktivitetTjeneste;
        this.beregningsgrunnlagDtoUtil = beregningsgrunnlagDtoUtil;
        this.refusjonOgGraderingTjeneste = refusjonOgGraderingTjeneste;
    }

    protected AndelerForFaktaOmBeregningTjeneste() {
        // For CDI
    }

    List<AndelForFaktaOmBeregningDto> lagAndelerForFaktaOmBeregning(BeregningsgrunnlagInput input) {
        var ref = input.getBehandlingReferanse();
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagFørFaktaAvklaringOpt = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(ref.getBehandlingId(), ref.getOriginalBehandlingId(), BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        if (grunnlagFørFaktaAvklaringOpt.isEmpty()) {
            return Collections.emptyList();
        }
        BeregningsgrunnlagGrunnlagEntitet grunnlagFørFaktaAvlaring = grunnlagFørFaktaAvklaringOpt.get();
        var beregningAktivitetAggregat = input.getBeregningsgrunnlagGrunnlag().getGjeldendeAktiviteter();
        BeregningsgrunnlagEntitet beregningsgrunnlag = finnRiktigBeregningsgrunnlag(ref, grunnlagFørFaktaAvlaring);
        List<BeregningsgrunnlagPrStatusOgAndel> andelerIFørstePeriode = beregningsgrunnlag
            .getBeregningsgrunnlagPerioder()
            .get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(a -> !refusjonOgGraderingTjeneste.erNyttArbeidsforhold(a, beregningAktivitetAggregat))
            .collect(Collectors.toList());
        return andelerIFørstePeriode.stream()
            .map(andel -> mapTilAndelIFaktaOmBeregning(input, andel))
            .collect(Collectors.toList());

    }

    private AndelForFaktaOmBeregningDto mapTilAndelIFaktaOmBeregning(BeregningsgrunnlagInput input, BeregningsgrunnlagPrStatusOgAndel andel) {
        var ref = input.getBehandlingReferanse();
        var inntektsmeldinger = input.getInntektsmeldinger();
        Optional<Inntektsmelding> inntektsmeldingForAndel = BeregningInntektsmeldingTjeneste.finnInntektsmeldingForAndel(andel, inntektsmeldinger);
        AndelForFaktaOmBeregningDto andelDto = new AndelForFaktaOmBeregningDto();
        andelDto.setFastsattBelop(FinnInntektForVisning.finnInntektForPreutfylling(andel));
        andelDto.setInntektskategori(andel.getInntektskategori());
        andelDto.setAndelsnr(andel.getAndelsnr());
        andelDto.setAktivitetStatus(andel.getAktivitetStatus());
        InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag = input.getIayGrunnlag();
        andelDto.setVisningsnavn(visningsnavnForAktivitetTjeneste.lagVisningsnavn(ref, inntektArbeidYtelseGrunnlag, andel));
        andelDto.setSkalKunneEndreAktivitet(SkalKunneEndreAktivitet.skalKunneEndreAktivitet(andel));
        andelDto.setLagtTilAvSaksbehandler(andel.getLagtTilAvSaksbehandler());
        beregningsgrunnlagDtoUtil.lagArbeidsforholdDto(andel, inntektsmeldingForAndel, inntektArbeidYtelseGrunnlag).ifPresent(andelDto::setArbeidsforhold);
        finnRefusjonskravFraInntektsmelding(inntektsmeldingForAndel).ifPresent(andelDto::setRefusjonskrav);
        FinnInntektForVisning.finnInntektForKunLese(ref, andel, inntektsmeldingForAndel, inntektArbeidYtelseGrunnlag).ifPresent(andelDto::setBelopReadOnly);
        return andelDto;
    }

    private Optional<BigDecimal> finnRefusjonskravFraInntektsmelding(Optional<Inntektsmelding> inntektsmeldingForAndel) {
        return inntektsmeldingForAndel
            .map(Inntektsmelding::getRefusjonBeløpPerMnd)
            .map(Beløp::getVerdi);
    }

    private BeregningsgrunnlagEntitet finnRiktigBeregningsgrunnlag(BehandlingReferanse ref, BeregningsgrunnlagGrunnlagEntitet grunnlagFørFaktaAvklaring) {
        String feilmelding = "Uviklerfeil: Grunnlag skal ha beregningsgrunnlag.";
        Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagEtterFaktaAvklaringOpt = beregningsgrunnlagRepository
            .hentSisteBeregningsgrunnlagGrunnlagEntitet(ref.getBehandlingId(), BeregningsgrunnlagTilstand.KOFAKBER_UT);
        BeregningsgrunnlagEntitet førFakta = grunnlagFørFaktaAvklaring
            .getBeregningsgrunnlag().orElseThrow(() -> new IllegalStateException(feilmelding));
        if (ref.erRevurdering()) {
            Boolean harSignifikantDiffMellomOriginalOgRevurdering = harDiffIBeregnigsgrunnlagFraOriginalBehandling(ref, førFakta);
            if (!harSignifikantDiffMellomOriginalOgRevurdering) {
                Optional<BeregningsgrunnlagGrunnlagEntitet> grunnlagEtterFaktaAvklaringForAlleBehandlingerOpt = finnSisteBekreftetGrunnlagOmFinnes(ref);
                return grunnlagEtterFaktaAvklaringForAlleBehandlingerOpt
                    .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
                    .orElse(førFakta);
            }
        }
        return grunnlagEtterFaktaAvklaringOpt
                .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
                .orElse(førFakta);
    }

    private Optional<BeregningsgrunnlagGrunnlagEntitet> finnSisteBekreftetGrunnlagOmFinnes(BehandlingReferanse ref) {
        return beregningsgrunnlagRepository
                        .hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(ref.getBehandlingId(), ref.getOriginalBehandlingId(), BeregningsgrunnlagTilstand.KOFAKBER_UT);
    }

    private Boolean harDiffIBeregnigsgrunnlagFraOriginalBehandling(BehandlingReferanse ref, BeregningsgrunnlagEntitet førFakta) {
        Long originalBehandlingId = ref.getOriginalBehandlingId().orElseThrow(() -> new IllegalStateException("Ingen forrige behandling id for revurdering"));
        return beregningsgrunnlagRepository
            .hentSisteBeregningsgrunnlagGrunnlagEntitet(originalBehandlingId, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag)
            .map(bg -> BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(førFakta, bg))
            .orElse(true);
    }

}
