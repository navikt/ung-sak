package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.Kopimaskin;
import no.nav.folketrygdloven.beregningsgrunnlag.MatchBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsattBrukersAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettBgKunYtelseDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("FASTSETT_BG_KUN_YTELSE")
public class FastsettBgKunYtelseOppdaterer implements FaktaOmBeregningTilfelleOppdaterer {

    private static final int MND_I_1_ÅR = 12;

    @Override
    public void oppdater(FaktaBeregningLagreDto dto, BehandlingReferanse behandlingReferanse,
                         BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                         Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        FastsettBgKunYtelseDto kunYtelseDto = dto.getKunYtelseFordeling();
        BeregningsgrunnlagPeriode periode = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<FastsattBrukersAndel> andeler = kunYtelseDto.getAndeler();
        fjernAndeler(periode, andeler.stream().map(FastsattBrukersAndel::getAndelsnr).collect(Collectors.toList()));
        Boolean skalBrukeBesteberegning = kunYtelseDto.getSkalBrukeBesteberegning();
        for (FastsattBrukersAndel andel : andeler) {
            if (andel.getNyAndel()) {
                fastsettBeløpForNyAndel(periode, andel, kunYtelseDto.getSkalBrukeBesteberegning());
            } else {
                BeregningsgrunnlagPrStatusOgAndel korrektAndel = Kopimaskin.deepCopy(getKorrektAndel(periode, andel, forrigeBg));
                settInntektskategoriOgFastsattBeløp(andel, korrektAndel, periode, skalBrukeBesteberegning);
            }
        }
    }


    private void fjernAndeler(BeregningsgrunnlagPeriode periode, List<Long> andelsnrListe) {
        BeregningsgrunnlagPeriode.builder(periode).fjernBeregningsgrunnlagPrStatusOgAndelerSomIkkeLiggerIListeAvAndelsnr(andelsnrListe);
    }


    private void settInntektskategoriOgFastsattBeløp(FastsattBrukersAndel andel, BeregningsgrunnlagPrStatusOgAndel korrektAndel,
                                             BeregningsgrunnlagPeriode periode, Boolean skalBrukeBesteberegning) {
        Inntektskategori inntektskategori = andel.getInntektskategori();
        BigDecimal fastsattBeløp = BigDecimal.valueOf(andel.getFastsattBeløp()*(long)MND_I_1_ÅR);
        if (andel.getNyAndel()) {
            BeregningsgrunnlagPrStatusOgAndel.builder(Kopimaskin.deepCopy(korrektAndel))
                .medBeregnetPrÅr(fastsattBeløp)
                .medBesteberegningPrÅr(Boolean.TRUE.equals(skalBrukeBesteberegning) ? fastsattBeløp : null)
                .medInntektskategori(inntektskategori)
                .medFastsattAvSaksbehandler(true)
                .nyttAndelsnr(periode)
                .medLagtTilAvSaksbehandler(true).build(periode);
        } else {
            Optional<BeregningsgrunnlagPrStatusOgAndel> matchetAndel = periode.getBeregningsgrunnlagPrStatusOgAndelList()
                .stream().filter(bgAndel -> bgAndel.equals(korrektAndel)).findFirst();
            matchetAndel.ifPresentOrElse(endreEksisterende(skalBrukeBesteberegning, inntektskategori, fastsattBeløp),
                leggTilFraForrige(korrektAndel, periode, skalBrukeBesteberegning, inntektskategori, fastsattBeløp)
            );
        }
    }

    private Runnable leggTilFraForrige(BeregningsgrunnlagPrStatusOgAndel korrektAndel, BeregningsgrunnlagPeriode periode, Boolean skalBrukeBesteberegning, Inntektskategori inntektskategori, BigDecimal fastsattBeløp) {
        return () ->  BeregningsgrunnlagPrStatusOgAndel.builder(korrektAndel)
            .medBeregnetPrÅr(fastsattBeløp)
            .medBesteberegningPrÅr(Boolean.TRUE.equals(skalBrukeBesteberegning) ? fastsattBeløp : null)
            .medInntektskategori(inntektskategori)
            .medFastsattAvSaksbehandler(true).build(periode);
    }

    private Consumer<BeregningsgrunnlagPrStatusOgAndel> endreEksisterende(Boolean skalBrukeBesteberegning, Inntektskategori inntektskategori, BigDecimal fastsattBeløp) {
        return match -> BeregningsgrunnlagPrStatusOgAndel.builder(match)
            .medBeregnetPrÅr(fastsattBeløp)
            .medBesteberegningPrÅr(Boolean.TRUE.equals(skalBrukeBesteberegning) ? fastsattBeløp : null)
            .medInntektskategori(inntektskategori)
            .medFastsattAvSaksbehandler(true);
    }


    private void fastsettBeløpForNyAndel(BeregningsgrunnlagPeriode periode,
                                         FastsattBrukersAndel andel, Boolean skalBrukeBesteberegning) {
        BigDecimal fastsatt = BigDecimal.valueOf(andel.getFastsattBeløp() * (long) MND_I_1_ÅR);// NOSONAR
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.BRUKERS_ANDEL)
            .medInntektskategori(andel.getInntektskategori())
            .medBeregnetPrÅr(fastsatt)
            .medBesteberegningPrÅr(Boolean.TRUE.equals(skalBrukeBesteberegning) ? fastsatt : null)
            .medFastsattAvSaksbehandler(true)
            .medLagtTilAvSaksbehandler(true)
            .build(periode);
    }


    private BeregningsgrunnlagPrStatusOgAndel getKorrektAndel(BeregningsgrunnlagPeriode periode, FastsattBrukersAndel andel, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        if (andel.getLagtTilAvSaksbehandler() && !andel.getNyAndel()) {
            return finnAndelFraForrigeGrunnlag(periode, andel, forrigeBg);
        }
        return periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(a -> a.getAndelsnr().equals(andel.getAndelsnr()))
            .findFirst()
            .orElseThrow(() -> FastsettBGKunYtelseOppdatererFeil.FACTORY.finnerIkkeAndelFeil().toException());
    }

    private BeregningsgrunnlagPrStatusOgAndel finnAndelFraForrigeGrunnlag(BeregningsgrunnlagPeriode periode, FastsattBrukersAndel andel, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        List<BeregningsgrunnlagPeriode> matchendePerioder = forrigeBg.stream()
            .flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .filter(periodeIGjeldendeGrunnlag -> periodeIGjeldendeGrunnlag.getPeriode().overlapper(periode.getPeriode())).collect(Collectors.toList());
        if (matchendePerioder.size() != 1) {
            throw MatchBeregningsgrunnlagTjeneste.MatchBeregningsgrunnlagTjenesteFeil.FACTORY.fantFlereEnn1Periode().toException();
        }
        Optional<BeregningsgrunnlagPrStatusOgAndel> andelIForrigeGrunnlag = matchendePerioder.get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(a -> a.getAndelsnr().equals(andel.getAndelsnr()))
            .findFirst();
        return andelIForrigeGrunnlag
            .orElseGet(() -> MatchBeregningsgrunnlagTjeneste
                .matchMedAndelFraPeriode(periode, andel.getAndelsnr(), null));
    }
}
