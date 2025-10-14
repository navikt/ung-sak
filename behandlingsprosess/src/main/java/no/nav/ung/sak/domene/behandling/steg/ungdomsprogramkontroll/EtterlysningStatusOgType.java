package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;

public record EtterlysningStatusOgType(
    EtterlysningStatus status,
    EtterlysningType type
) {
}
