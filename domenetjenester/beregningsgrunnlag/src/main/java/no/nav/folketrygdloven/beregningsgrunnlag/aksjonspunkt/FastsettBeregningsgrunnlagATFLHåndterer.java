package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagATFLDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.InntektPrAndelDto;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class FastsettBeregningsgrunnlagATFLHåndterer {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    FastsettBeregningsgrunnlagATFLHåndterer() {
        // for CDI proxy
    }

    @Inject
    public FastsettBeregningsgrunnlagATFLHåndterer(BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    public void håndter(Long behandlingId, FastsettBeregningsgrunnlagATFLDto dto) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingId);
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = beregningsgrunnlag.dypKopi();
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        List<InntektPrAndelDto> fastsattInntektListe = dto.getInntektPrAndelList();
        BeregningsgrunnlagPeriode førstePeriode = perioder.get(0);
        List<BeregningsgrunnlagPrStatusOgAndel> arbeidstakerList = førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER))
            .collect(Collectors.toList());
        if (fastsattInntektListe != null && !arbeidstakerList.isEmpty()) {
            for (InntektPrAndelDto inntekPrAndel : fastsattInntektListe) {
                BeregningsgrunnlagPrStatusOgAndel korresponderendeAndelIFørstePeriode = arbeidstakerList.stream()
                    .filter(andel -> andel.getAndelsnr().equals(inntekPrAndel.getAndelsnr()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Fant ingen korresponderende andel med andelsnr " + inntekPrAndel.getAndelsnr() + " i første periode for behandling " + behandlingId));
                for (BeregningsgrunnlagPeriode periode : perioder) {
                    Optional<BeregningsgrunnlagPrStatusOgAndel> korresponderendeAndelOpt = finnRiktigAndel(korresponderendeAndelIFørstePeriode, periode);
                    korresponderendeAndelOpt.ifPresent(andel-> BeregningsgrunnlagPrStatusOgAndel.builder(andel)
                        .medOverstyrtPrÅr(BigDecimal.valueOf(inntekPrAndel.getInntekt())));
                }
            }
        }
        if (dto.getInntektFrilanser() != null) {
            for (BeregningsgrunnlagPeriode periode : perioder) {
                List<BeregningsgrunnlagPrStatusOgAndel> frilanserList = periode.getBeregningsgrunnlagPrStatusOgAndelList()
                    .stream()
                    .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.FRILANSER))
                    .collect(Collectors.toList());
                frilanserList.forEach(prStatusOgAndel ->
                    BeregningsgrunnlagPrStatusOgAndel.builder(prStatusOgAndel).medOverstyrtPrÅr(BigDecimal.valueOf(dto.getInntektFrilanser())));
            }
        }
        beregningsgrunnlagRepository.lagre(behandlingId, nyttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT_UT);
    }

    private Optional<BeregningsgrunnlagPrStatusOgAndel> finnRiktigAndel(BeregningsgrunnlagPrStatusOgAndel andelIFørstePeriode, BeregningsgrunnlagPeriode periode) {
        return periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(andel -> andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER))
            .filter(andel -> andel.equals(andelIFørstePeriode)).findFirst();
    }

}
