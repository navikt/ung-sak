package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import java.util.Objects;
import java.util.Set;

public class RapporterteInntekter {

    private Set<RapportertInntekt> rapporterteInntekter;


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

}
