package no.nav.k9.sak.web.app.tjenester.dokument;

import java.time.LocalDateTime;
import java.util.Objects;

class BehandlingPeriode implements Comparable<BehandlingPeriode> {

    private LocalDateTime fom;
    private LocalDateTime tom;
    private Long behandlingId;

    public BehandlingPeriode(LocalDateTime fom, LocalDateTime tom, Long behandlingId) {
        this.fom = Objects.requireNonNull(fom);
        this.tom = Objects.requireNonNull(tom);
        this.behandlingId = behandlingId;
    }

    public LocalDateTime getFom() {
        return fom;
    }

    public LocalDateTime getTom() {
        return tom;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    @Override
    public int compareTo(BehandlingPeriode periode) {
        return getFom().compareTo(periode.getFom());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BehandlingPeriode that = (BehandlingPeriode) o;
        return Objects.equals(fom, that.fom) && Objects.equals(tom, that.tom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fom, tom);
    }

    @Override
    public String toString() {
        return "BehandlingPeriode{" +
            "fom=" + fom +
            ", tom=" + tom +
            '}';
    }
}
