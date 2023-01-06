package no.nav.k9.sak.ytelse.omsorgspenger.stønadsstatistikk;

import java.util.Objects;
import java.util.Set;

import no.nav.k9.kodeverk.vilkår.Utfall;

public class VilkårUtfall {

    private final Utfall utfall;

    private final Set<DetaljertVilkårUtfall> detaljer;

    public VilkårUtfall(Utfall utfall) {
        this.utfall = utfall;
        this.detaljer = null;
    }

    public VilkårUtfall(Utfall utfall, Set<DetaljertVilkårUtfall> detaljer) {
        this.utfall = utfall;
        this.detaljer = detaljer;
    }

    public Utfall getUtfall() {
        return utfall;
    }

    public Set<DetaljertVilkårUtfall> getDetaljer() {
        return detaljer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VilkårUtfall that = (VilkårUtfall) o;
        return utfall == that.utfall && Objects.equals(detaljer, that.detaljer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(utfall, detaljer);
    }

    @Override
    public String toString() {
        return "VilkårUtfall{" +
            "utfall=" + utfall +
            (detaljer != null ? ", detaljer=" + detaljer : "") +
            '}';
    }
}
