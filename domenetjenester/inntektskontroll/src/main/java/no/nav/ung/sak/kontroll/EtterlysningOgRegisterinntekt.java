package no.nav.ung.sak.kontroll;


import java.util.Set;

public record EtterlysningOgRegisterinntekt(Set<RapportertInntekt> registerInntekt, InntektskontrollEtterlysningInfo etterlysning) {
}
