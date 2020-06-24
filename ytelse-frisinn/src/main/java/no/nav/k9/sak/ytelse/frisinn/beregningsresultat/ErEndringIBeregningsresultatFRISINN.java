package no.nav.k9.sak.ytelse.frisinn.beregningsresultat;

import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.frisinn.mapper.FrisinnSøknadsperiodeMapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ErEndringIBeregningsresultatFRISINN {

    private ErEndringIBeregningsresultatFRISINN() {
        // Skjuler default
    }

    public static boolean erUgunst(Optional<BeregningsresultatEntitet> revurderingsGrunnlag, Optional<BeregningsresultatEntitet> originaltGrunnlag, UttakAktivitet orginaltUttak) {

        if (revurderingsGrunnlag.isEmpty()) {
            return originaltGrunnlag.isPresent();
        }

        Iterator<Periode> uttaksperioder = FrisinnSøknadsperiodeMapper.map(orginaltUttak).iterator();

        List<BeregningsresultatPeriode> originalePerioder = originaltGrunnlag.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList());
        List<BeregningsresultatPeriode> revurderingsPerioder = revurderingsGrunnlag.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList());

        while (uttaksperioder.hasNext()) {
            Periode periode = uttaksperioder.next();
            BigDecimal orginalUtbetalingIPerioden = finnUtbetalingIPerioden(periode, originalePerioder);
            BigDecimal revurderingUtbetalingIPerioden = finnUtbetalingIPerioden(periode, revurderingsPerioder);
            if (orginalUtbetalingIPerioden.compareTo(revurderingUtbetalingIPerioden) > 0) {
                return true;
            }
        }
        return false;
    }

    private static BigDecimal finnUtbetalingIPerioden(Periode uttaksperiode, List<BeregningsresultatPeriode> brPerioder) {
        List<BeregningsresultatPeriode> overlappendeBGPerioder = brPerioder.stream()
            .filter(bgp -> !bgp.getBeregningsresultatPeriodeFom().isBefore(uttaksperiode.getFom())
                && !bgp.getBeregningsresultatPeriodeTom().isAfter(uttaksperiode.getTom()))
            .collect(Collectors.toList());
        return overlappendeBGPerioder.stream()
            .map(ErEndringIBeregningsresultatFRISINN::utbetalingIPerioden).reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);
    }

    private static BigDecimal utbetalingIPerioden(BeregningsresultatPeriode periode) {
        BigDecimal dagsats = BigDecimal.valueOf(periode.getDagsats());
        BigDecimal antallArbeidsdager = BigDecimal.valueOf(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom()).antallArbeidsdager());
        return dagsats.multiply(antallArbeidsdager);
    }
}
