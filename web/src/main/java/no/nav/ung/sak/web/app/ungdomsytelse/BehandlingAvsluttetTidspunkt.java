package no.nav.ung.sak.web.app.ungdomsytelse;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

public record BehandlingAvsluttetTidspunkt(LocalDateTime avsluttetTid) implements Comparable<BehandlingAvsluttetTidspunkt> {

    public static BehandlingAvsluttetTidspunkt fraBehandling(Behandling behandling) {
        return new BehandlingAvsluttetTidspunkt(behandling.getAvsluttetDato());
    }

    public boolean erAvsluttet() {
        return avsluttetTid != null;
    }

    public LocalDateTime getAvsluttetTid() {
        return avsluttetTid;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BehandlingAvsluttetTidspunkt(LocalDateTime tid))) return false;
        return Objects.equals(avsluttetTid, tid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(avsluttetTid);
    }

    @Override
    public int compareTo(@NotNull BehandlingAvsluttetTidspunkt o) {
        if (this.erAvsluttet() && o.erAvsluttet()) {
            return this.avsluttetTid.compareTo(o.avsluttetTid);
        }
        else if (!this.erAvsluttet() && o.erAvsluttet()) {
            return 1;
        }
        return -1;
    }
}
