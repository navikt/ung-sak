package no.nav.k9.sak.ytelse.frisinn.beregningsresultat;

import static no.nav.k9.sak.ytelse.frisinn.beregningsresultat.ErEndringIBeregningsresultatFRISINN.BeregningsresultatEndring.GUNST;
import static no.nav.k9.sak.ytelse.frisinn.beregningsresultat.ErEndringIBeregningsresultatFRISINN.BeregningsresultatEndring.INGEN_ENDRING;
import static no.nav.k9.sak.ytelse.frisinn.beregningsresultat.ErEndringIBeregningsresultatFRISINN.BeregningsresultatEndring.UGUNST;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.frisinn.mapper.FrisinnSøknadsperiodeMapper;

public class ErEndringIBeregningsresultatFRISINN {

    public enum BeregningsresultatEndring {
        GUNST,
        UGUNST,
        INGEN_ENDRING
    }

    private ErEndringIBeregningsresultatFRISINN() {
        // Skjuler default
    }

    public static List<BeregningsresultatEndring> finnEndringerIUtbetalinger(Optional<BeregningsresultatEntitet> revurderingResultat, Optional<BeregningsresultatEntitet> originaltResultat, UttakAktivitet orginaltUttak) {
        if (revurderingResultat.isEmpty()) {
            var endring = originaltResultat.isPresent() ? UGUNST : INGEN_ENDRING;
            return List.of(endring);
        }
        if (originaltResultat.isEmpty()) {
            var endring = revurderingResultat.isPresent() ? GUNST : INGEN_ENDRING;
            return List.of(endring);
        }

        var originalePerioder = originaltResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList());
        var revurderingsPerioder = revurderingResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList());
        var uttaksperioder = FrisinnSøknadsperiodeMapper.map(orginaltUttak);

        return uttaksperioder.stream()
            .map(periode -> {
                BigDecimal orginalUtbetalingIPerioden = finnUtbetalingIPerioden(periode, originalePerioder);
                BigDecimal revurderingUtbetalingIPerioden = finnUtbetalingIPerioden(periode, revurderingsPerioder);
                var sammenligning = orginalUtbetalingIPerioden.compareTo(revurderingUtbetalingIPerioden);
                return sammenligning > 0 ? UGUNST : sammenligning < 0 ? GUNST : INGEN_ENDRING;
            })
            .collect(Collectors.toList());
    }

    private static BigDecimal finnUtbetalingIPerioden(Periode uttaksperiode, List<BeregningsresultatPeriode> brPerioder) {
        List<BeregningsresultatPeriode> overlappendeBGPerioder = brPerioder.stream()
            .filter(bgp -> {
                ÅpenDatoIntervallEntitet bgResperiode = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(bgp.getBeregningsresultatPeriodeFom(), bgp.getBeregningsresultatPeriodeTom());
                return bgResperiode.overlapper(uttaksperiode.getFom(), uttaksperiode.getTom());
            })
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
