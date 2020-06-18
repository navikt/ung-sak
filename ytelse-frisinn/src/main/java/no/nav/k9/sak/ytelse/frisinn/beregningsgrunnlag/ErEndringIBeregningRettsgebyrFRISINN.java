package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.frisinn.mapper.FrisinnSøknadsperiodeMapper;

public class ErEndringIBeregningRettsgebyrFRISINN {

  public final static BigDecimal TOLERANSE_GRENSE_DAGSATS = BigDecimal.valueOf(1172);

    private ErEndringIBeregningRettsgebyrFRISINN() {
        // Skjuler default
    }

    public static boolean erUgunst(Optional<Beregningsgrunnlag> revurderingsGrunnlag, Optional<Beregningsgrunnlag> originaltGrunnlag, UttakAktivitet orginaltUttak) {

        if (revurderingsGrunnlag.isEmpty()) {
            return originaltGrunnlag.isPresent();
        }

        Iterator<Periode> uttaksperioder = FrisinnSøknadsperiodeMapper.map(orginaltUttak).iterator();

        List<BeregningsgrunnlagPeriode> originalePerioder = originaltGrunnlag.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder).orElse(Collections.emptyList());
        List<BeregningsgrunnlagPeriode> revurderingsPerioder = revurderingsGrunnlag.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder).orElse(Collections.emptyList());
        BigDecimal totalUgunst = BigDecimal.ZERO;
        while (uttaksperioder.hasNext()) {
            Periode periode = uttaksperioder.next();
            BigDecimal orginalUtbetalingIPerioden = finnUtbetalingIPerioden(periode, originalePerioder);
            BigDecimal revurderingUtbetalingIPerioden = finnUtbetalingIPerioden(periode, revurderingsPerioder);
            var ugunstIPerioden = orginalUtbetalingIPerioden.subtract(revurderingUtbetalingIPerioden).max(BigDecimal.ZERO);
            totalUgunst = totalUgunst.add(ugunstIPerioden);
        }
        return totalUgunst.compareTo(TOLERANSE_GRENSE_DAGSATS) >= 0;
    }

    private static BigDecimal finnUtbetalingIPerioden(Periode uttaksperiode, List<BeregningsgrunnlagPeriode> bgPerioder) {
        ÅpenDatoIntervallEntitet uttak = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(uttaksperiode.getFom(), uttaksperiode.getTom());
        List<BeregningsgrunnlagPeriode> overlappendeBGPerioder = bgPerioder.stream()
            .filter(bgp -> uttak.overlapper(bgp.getPeriode()))
            .collect(Collectors.toList());
        return overlappendeBGPerioder.stream()
            .map(ErEndringIBeregningRettsgebyrFRISINN::utbetalingIPerioden).reduce(BigDecimal::add)
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
