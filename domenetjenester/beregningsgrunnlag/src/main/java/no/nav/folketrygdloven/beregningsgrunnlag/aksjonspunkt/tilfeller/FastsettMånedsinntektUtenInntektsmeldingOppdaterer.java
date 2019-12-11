package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.FastsettBeregningVerdierTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsatteVerdierDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettMånedsinntektUtenInntektsmeldingAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettMånedsinntektUtenInntektsmeldingDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.RedigerbarAndelFaktaOmBeregningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("FASTSETT_MÅNEDSLØNN_ARBEIDSTAKER_UTEN_INNTEKTSMELDING")
public class FastsettMånedsinntektUtenInntektsmeldingOppdaterer implements FaktaOmBeregningTilfelleOppdaterer {

    @Override
    public void oppdater(FaktaBeregningLagreDto dto, BehandlingReferanse behandlingReferanse, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                         Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        FastsettMånedsinntektUtenInntektsmeldingDto fastsettMånedsinntektDto = dto.getFastsattUtenInntektsmelding();
        List<BeregningsgrunnlagPrStatusOgAndel> arbeidstakerAndeleriFørstePeriode = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(bpsa -> bpsa.getAktivitetStatus().erArbeidstaker())
            .collect(Collectors.toList());
        List<FastsettMånedsinntektUtenInntektsmeldingAndelDto> andelListe = fastsettMånedsinntektDto.getAndelListe();
        settInntektForAllePerioder(nyttBeregningsgrunnlag, forrigeBg, arbeidstakerAndeleriFørstePeriode, andelListe);
    }

    private void settInntektForAllePerioder(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                            Optional<BeregningsgrunnlagEntitet> forrigeBg,
                                            List<BeregningsgrunnlagPrStatusOgAndel> arbeidstakerAndeleriFørstePeriode,
                                            List<FastsettMånedsinntektUtenInntektsmeldingAndelDto> andelListe) {
        for (FastsettMånedsinntektUtenInntektsmeldingAndelDto dtoAndel : andelListe) {
            BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagAndel = finnKorrektAndel(arbeidstakerAndeleriFørstePeriode, dtoAndel);
            for (BeregningsgrunnlagPeriode periode : nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder()) {
                Optional<BeregningsgrunnlagPeriode> forrigePeriode = finnForrigePeriode(forrigeBg, periode);
                Optional<BeregningsgrunnlagPrStatusOgAndel> andelForArbeidsforhold = finnAndelIPeriode(beregningsgrunnlagAndel, periode);
                if (andelForArbeidsforhold.isPresent()) {
                    RedigerbarAndelFaktaOmBeregningDto redigerbarAndel = lagRedigerbarAndel(andelForArbeidsforhold.get());
                    FastsettBeregningVerdierTjeneste.fastsettVerdierForAndel(redigerbarAndel, mapTilFastsatteVerdier(andelForArbeidsforhold.get(), dtoAndel), periode, forrigePeriode);
                }
            }
        }
    }

    private FastsatteVerdierDto mapTilFastsatteVerdier(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagPrStatusOgAndel, FastsettMånedsinntektUtenInntektsmeldingAndelDto dtoAndel) {
        return new FastsatteVerdierDto(dtoAndel.getFastsattBeløp(), beregningsgrunnlagPrStatusOgAndel.getInntektskategori());
    }

    private RedigerbarAndelFaktaOmBeregningDto lagRedigerbarAndel(BeregningsgrunnlagPrStatusOgAndel andelForArbeidsforhold) {
        return new RedigerbarAndelFaktaOmBeregningDto(false, andelForArbeidsforhold.getAndelsnr(), false);
    }

    private Optional<BeregningsgrunnlagPrStatusOgAndel> finnAndelIPeriode(BeregningsgrunnlagPrStatusOgAndel beregningsgrunnlagAndel, BeregningsgrunnlagPeriode periode) {
        return periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(andel -> andel.gjelderSammeArbeidsforhold(beregningsgrunnlagAndel)).findFirst();
    }

    private Optional<BeregningsgrunnlagPeriode> finnForrigePeriode(Optional<BeregningsgrunnlagEntitet> forrigeBg, BeregningsgrunnlagPeriode periode) {
        return forrigeBg.stream()
            .flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .filter(p -> periode.getPeriode().inkluderer(p.getPeriode().getFomDato())).findFirst();
    }

    private BeregningsgrunnlagPrStatusOgAndel finnKorrektAndel(List<BeregningsgrunnlagPrStatusOgAndel> arbeidstakerAndeler, FastsettMånedsinntektUtenInntektsmeldingAndelDto dtoAndel) {
        return arbeidstakerAndeler.stream()
            .filter(bgAndel -> dtoAndel.getAndelsnr().equals(bgAndel.getAndelsnr()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke andel for andelsnr " + dtoAndel.getAndelsnr()));
    }
}
