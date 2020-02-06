package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FastsatteAndelerTidsbegrensetDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FastsattePerioderTidsbegrensetDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FastsettBGTidsbegrensetArbeidsforholdDto;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class FastsettBGTidsbegrensetArbeidsforholdHåndterer {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    FastsettBGTidsbegrensetArbeidsforholdHåndterer() {
        // for CDI proxy
    }

    @Inject
    public FastsettBGTidsbegrensetArbeidsforholdHåndterer(BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    public void håndter(Long behandlingId, FastsettBGTidsbegrensetArbeidsforholdDto dto) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagAggregatForBehandling(behandlingId);
        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = beregningsgrunnlag.dypKopi();
        List<BeregningsgrunnlagPeriode> perioder = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        List<FastsattePerioderTidsbegrensetDto> fastsattePerioder = dto.getFastsatteTidsbegrensedePerioder();
        if (dto.getFrilansInntekt() != null) {
            for (BeregningsgrunnlagPeriode periode : perioder) {
                BeregningsgrunnlagPrStatusOgAndel frilansAndel = finnFrilansAndel(periode)
                    .orElseThrow(() -> new IllegalStateException("Mangler frilansandel for behandling " + behandlingId));
                BeregningsgrunnlagPrStatusOgAndel.builder(frilansAndel).medOverstyrtPrÅr(BigDecimal.valueOf(dto.getFrilansInntekt()));
            }
        }
        for (FastsattePerioderTidsbegrensetDto periode: fastsattePerioder) {
            List<BeregningsgrunnlagPeriode> bgPerioderSomSkalFastsettesAvDennePerioden = perioder
                .stream()
                .filter(p -> !p.getBeregningsgrunnlagPeriodeFom().isBefore(periode.getPeriodeFom()))
                .collect(Collectors.toList());
            List<FastsatteAndelerTidsbegrensetDto> fastatteAndeler = periode.getFastsatteTidsbegrensedeAndeler();
            fastatteAndeler.forEach(andel ->
                fastsettAndelerIPeriode(bgPerioderSomSkalFastsettesAvDennePerioden, andel));
        }
        beregningsgrunnlagRepository.lagre(behandlingId, nyttBeregningsgrunnlag, BeregningsgrunnlagTilstand.FORESLÅTT_UT);
    }

    private Optional<BeregningsgrunnlagPrStatusOgAndel> finnFrilansAndel(BeregningsgrunnlagPeriode periode) {
        return periode.getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.FRILANSER))
            .findFirst();
    }

    private void fastsettAndelerIPeriode(List<BeregningsgrunnlagPeriode> bgPerioderSomSkalFastsettesAvDennePerioden, FastsatteAndelerTidsbegrensetDto andel) {
        bgPerioderSomSkalFastsettesAvDennePerioden.forEach(p -> {
            Optional<BeregningsgrunnlagPrStatusOgAndel> korrektAndel = p.getBeregningsgrunnlagPrStatusOgAndelList().stream().filter(a -> a.getAndelsnr().equals(andel.getAndelsnr())).findFirst();
            korrektAndel.ifPresent(beregningsgrunnlagPrStatusOgAndel -> BeregningsgrunnlagPrStatusOgAndel
                .builder(beregningsgrunnlagPrStatusOgAndel)
                .medOverstyrtPrÅr(BigDecimal.valueOf(andel.getBruttoFastsattInntekt())));
        });
    }

}
