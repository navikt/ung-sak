package no.nav.ung.sak.ytelse;

import no.nav.ung.sak.uttalelse.EtterlysningInfo;

import java.util.Set;

public record EtterlysningOgRegisterinntekt(Set<RapportertInntekt> registerInntekt, EtterlysningInfo etterlysning) {
}
