package no.nav.ung.ytelse.vurderkompletthet.ungdomsprogramkontroll;

import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;

public record EtterlysningStatusOgType(
    EtterlysningStatus status,
    EtterlysningType type
) {
}
