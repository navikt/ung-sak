package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedSøkerInfoDto;
import no.nav.k9.sak.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.ytelse.frisinn.mapper.FrisinnMapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ErEndringIBeregningFRISINN {

    private ErEndringIBeregningFRISINN() {
        // Skjuler default
    }

    public static boolean vurder(Optional<Beregningsgrunnlag> revurderingsGrunnlag, Optional<Beregningsgrunnlag> originaltGrunnlag, UttakAktivitet orginaltUttak) {
        if (revurderingsGrunnlag.isEmpty()) {
            return originaltGrunnlag.isPresent();
        }

        Iterator<PeriodeMedSøkerInfoDto> uttaksperioder = FrisinnMapper.mapPeriodeMedSøkerInfoDto(orginaltUttak).iterator();

        List<BeregningsgrunnlagPeriode> originalePerioder = originaltGrunnlag.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder).orElse(Collections.emptyList());
        List<BeregningsgrunnlagPeriode> revurderingsPerioder = revurderingsGrunnlag.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder).orElse(Collections.emptyList());

        while (uttaksperioder.hasNext()) {
            PeriodeMedSøkerInfoDto periode = uttaksperioder.next();
            BigDecimal orginalUtbetalingIPerioden = finnUtbetalingIPerioden(periode, originalePerioder);
            BigDecimal revurderingUtbetalingIPerioden = finnUtbetalingIPerioden(periode, revurderingsPerioder);
            if (orginalUtbetalingIPerioden.compareTo(revurderingUtbetalingIPerioden) < 0) {
                return true;
            }
        }
        return false;
    }

    private static BigDecimal finnUtbetalingIPerioden(PeriodeMedSøkerInfoDto uttaksperiode, List<BeregningsgrunnlagPeriode> bgPerioder) {
        List<BeregningsgrunnlagPeriode> overlappendeBGPerioder = bgPerioder.stream()
            .filter(bgp -> !bgp.getBeregningsgrunnlagPeriodeFom().isBefore(uttaksperiode.getPeriode().getFom())
                && !bgp.getBeregningsgrunnlagPeriodeTom().isAfter(uttaksperiode.getPeriode().getTom()))
            .collect(Collectors.toList());
        return overlappendeBGPerioder.stream()
            .map(ErEndringIBeregningFRISINN::utbetalingIPerioden).reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal utbetalingIPerioden(BeregningsgrunnlagPeriode periode) {
        if (periode.getDagsats() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal dagsats = BigDecimal.valueOf(periode.getDagsats());
        BigDecimal antallArbeidsdager = BigDecimal.valueOf(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periode.getBeregningsgrunnlagPeriodeFom(), periode.getBeregningsgrunnlagPeriodeTom()).antallArbeidsdager());
        return dagsats.multiply(antallArbeidsdager);
    }
}
