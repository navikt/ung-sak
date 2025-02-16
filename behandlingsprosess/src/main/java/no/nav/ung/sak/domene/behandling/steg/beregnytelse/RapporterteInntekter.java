package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import no.nav.ung.sak.behandlingslager.behandling.sporing.Sporingsverdi;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class RapporterteInntekter extends Sporingsverdi {

    private Set<RapportertInntekt> rapporterteInntekter = new HashSet<>();


    public RapporterteInntekter(Set<RapportertInntekt> rapporterteInntekter) {
        this.rapporterteInntekter = rapporterteInntekter;
    }

    public Set<RapportertInntekt> getRapporterteInntekter() {
        return rapporterteInntekter;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RapporterteInntekter that)) return false;
        return Objects.equals(rapporterteInntekter, that.rapporterteInntekter);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rapporterteInntekter);
    }

    @Override
    public String toString() {
        return "RapporterteInntekter{" +
            "rapporterteInntekter=" + rapporterteInntekter +
            '}';
    }

    @Override
    public String tilRegelVerdi() {
        return "rapporterteInntekter: [" + String.join(",", rapporterteInntekter.stream().map(RapportertInntekt::tilRegelVerdi).toList()) + "]";
    }
}
