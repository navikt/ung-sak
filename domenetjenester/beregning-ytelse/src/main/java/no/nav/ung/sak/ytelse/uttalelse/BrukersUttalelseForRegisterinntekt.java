package no.nav.ung.sak.ytelse.uttalelse;

import no.nav.ung.sak.ytelse.RapportertInntekt;

import java.util.Set;

public record BrukersUttalelseForRegisterinntekt(Status status, Set<RapportertInntekt> registerInntekt, Uttalelse uttalelse) {
}
