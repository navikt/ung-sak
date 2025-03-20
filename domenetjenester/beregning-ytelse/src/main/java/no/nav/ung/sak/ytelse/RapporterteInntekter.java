package no.nav.ung.sak.ytelse;

import java.util.Set;

public record RapporterteInntekter(
    Set<RapportertInntekt> brukerRapporterteInntekter,
    Set<RapportertInntekt> registerRapporterteInntekter) {

}
