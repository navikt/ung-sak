package no.nav.ung.sak.ytelse;

import no.nav.ung.sak.uttalelse.Status;
import no.nav.ung.sak.uttalelse.Uttalelse;

import java.util.Set;

public record BrukersUttalelseForRegisterinntekt(Status status, Set<RapportertInntekt> registerInntekt, Uttalelse uttalelse) {
}
