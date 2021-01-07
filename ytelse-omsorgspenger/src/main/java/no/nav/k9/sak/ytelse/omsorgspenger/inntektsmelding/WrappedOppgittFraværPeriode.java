package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

import java.time.LocalDateTime;
import java.util.Objects;

public class WrappedOppgittFraværPeriode {
    private OppgittFraværPeriode periode;
    private LocalDateTime innsendingstidspunkt;
    private Utfall søknadsfristUtfall;

    public WrappedOppgittFraværPeriode(OppgittFraværPeriode periode, LocalDateTime innsendingstidspunkt, Utfall søknadsfristUtfall) {
        this.periode = periode;
        this.innsendingstidspunkt = innsendingstidspunkt;
        this.søknadsfristUtfall = søknadsfristUtfall;
    }

    public OppgittFraværPeriode getPeriode() {
        return periode;
    }

    public Utfall getSøknadsfristUtfall() {
        return søknadsfristUtfall;
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
