package no.nav.ung.sak.ytelse;

import java.util.Objects;
import java.util.Set;

public class RapporterteInntekter {

    private Set<RapportertInntekt> brukerRapporterteInntekter;
    private Set<RapportertInntekt> registerRapporterteInntekter;


    public RapporterteInntekter(Set<RapportertInntekt> brukerRapporterteInntekter, Set<RapportertInntekt> registerRapporterteInntekter) {
        this.brukerRapporterteInntekter = brukerRapporterteInntekter;
        this.registerRapporterteInntekter = registerRapporterteInntekter;
    }

    public Set<RapportertInntekt> getBrukerRapporterteInntekter() {
        return brukerRapporterteInntekter;
    }

    public Set<RapportertInntekt> getRegisterRapporterteInntekter() {
        return registerRapporterteInntekter;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RapporterteInntekter that)) return false;
        return Objects.equals(brukerRapporterteInntekter, that.brukerRapporterteInntekter) && Objects.equals(registerRapporterteInntekter, that.registerRapporterteInntekter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(brukerRapporterteInntekter, registerRapporterteInntekter);
    }

    @Override
    public String toString() {
        return "RapporterteInntekter{" +
            "brukerRapporterteInntekter=" + brukerRapporterteInntekter +
            ", registerRapporterteInntekter=" + registerRapporterteInntekter +
            '}';
    }
}
