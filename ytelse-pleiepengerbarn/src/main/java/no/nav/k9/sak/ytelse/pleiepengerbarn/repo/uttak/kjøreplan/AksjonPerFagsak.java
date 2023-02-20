package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan;

import java.util.Objects;

public class AksjonPerFagsak {

    private final long fagsakId;
    private final Aksjon aksjon;

    public AksjonPerFagsak(long fagsakId, Aksjon aksjon) {
        this.fagsakId = fagsakId;
        this.aksjon = Objects.requireNonNull(aksjon);
    }

    public long getFagsakId() {
        return fagsakId;
    }

    public Aksjon getAksjon() {
        return aksjon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AksjonPerFagsak that = (AksjonPerFagsak) o;
        return fagsakId == that.fagsakId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fagsakId);
    }

    @Override
    public String toString() {
        return "AksjonPerFagsak{" +
            "fagsakId=" + fagsakId +
            ", aksjon=" + aksjon +
            '}';
    }
}
