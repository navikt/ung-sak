package no.nav.k9.sak.behandlingskontroll;

import java.util.Objects;

import no.nav.k9.kodeverk.behandling.BehandlingStegStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;

public class BehandlingStegTilstandSnapshot {

    private final Long id;
    private final BehandlingStegType steg;
    private final BehandlingStegStatus status;

    public BehandlingStegTilstandSnapshot(Long id, BehandlingStegType steg, BehandlingStegStatus status) {
        this.id = id;
        this.steg = steg;
        this.status = status;
    }

    public BehandlingStegType getSteg() {
        return steg;
    }

    public BehandlingStegStatus getStatus() {
        return status;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BehandlingStegTilstandSnapshot that = (BehandlingStegTilstandSnapshot) o;
        return Objects.equals(steg, that.steg) &&
            Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(steg, status);
    }

    @Override
    public String toString() {
        return "BehandlingStegTilstandSnapshot{" +
            "id=" + id +
            ", steg=" + steg +
            ", status=" + status +
            '}';
    }
}
