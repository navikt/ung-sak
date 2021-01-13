package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;

public class KalkulatorForBeregningsresultat {

    private FagsakYtelseType ytelseType;

    KalkulatorForBeregningsresultat(FagsakYtelseType ytelseType) {
        this.ytelseType = ytelseType;
    }

    public long beregnTotalsum(BeregningsresultatEntitet beregningsresultatEntitet) {
        return beregningsresultatEntitet.getBeregningsresultatPerioder().stream()
            .mapToLong(b -> beregnAntallYtelsedager(b) * b.getDagsats())
            .sum();
    }

    private long beregnAntallYtelsedager(BeregningsresultatPeriode periode) {
        return ytelseType == FagsakYtelseType.OMSORGSPENGER
            ? beregnKalanderdager(periode)
            : beregnUkedager(periode);
    }

    private static long beregnKalanderdager(BeregningsresultatPeriode periode) {
        return ChronoUnit.DAYS.between(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom()) + 1;
    }

    private static int beregnUkedager(BeregningsresultatPeriode periode) {
        return beregnUkedager(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom());
    }

    private static int beregnUkedager(LocalDate fom, LocalDate tom) {
        int antallUkedager = 0;
        for (LocalDate d = fom; !d.isAfter(tom); d = d.plusDays(1)) {
            int dag = d.getDayOfWeek().getValue();
            if (dag <= DayOfWeek.FRIDAY.getValue()) {
                antallUkedager++;
            }
        }
        return antallUkedager;
    }
}
