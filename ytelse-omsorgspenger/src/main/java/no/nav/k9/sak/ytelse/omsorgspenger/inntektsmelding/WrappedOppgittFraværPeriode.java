package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.time.LocalDateTime;
import java.util.Objects;

import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class WrappedOppgittFraværPeriode {
    private OppgittFraværPeriode periode;
    private LocalDateTime innsendingstidspunkt;

    public WrappedOppgittFraværPeriode(OppgittFraværPeriode periode, LocalDateTime innsendingstidspunkt) {
        this.periode = periode;
        this.innsendingstidspunkt = innsendingstidspunkt;
    }

    public OppgittFraværPeriode getPeriode() {
        return periode;
    }

    public LocalDateTime getInnsendingstidspunkt() {
        return innsendingstidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WrappedOppgittFraværPeriode that = (WrappedOppgittFraværPeriode) o;
        return periodeEquals(that);
    }

    private boolean periodeEquals(WrappedOppgittFraværPeriode that) {
        if (this.periode != null && that.periode != null) {
            return this.periode.equals(that.periode);
        } else
            return this.periode == null && that.periode == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode.hashCode());
    }

    @Override
    public String toString() {
        return "WrappedOppgittFraværPeriode{" +
            "periode=" + periode +
            '}';
    }
}
