package no.nav.ung.sak.kontroll;


import java.util.Set;

public record RapporterteInntekter(
    Set<RapportertInntekt> brukerRapporterteInntekter,
    Set<RapportertInntekt> registerRapporterteInntekter) {

    public static RapporterteInntekter utenRapporterteInntekter() {
        return new RapporterteInntekter(Set.of(), Set.of());
    }

}
