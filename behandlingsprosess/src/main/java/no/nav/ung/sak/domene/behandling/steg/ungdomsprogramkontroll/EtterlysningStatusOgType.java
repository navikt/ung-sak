package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import no.nav.ung.kodeverk.forhåndsvarsel.EtterlysningStatus;
import no.nav.ung.kodeverk.forhåndsvarsel.EtterlysningType;

public record EtterlysningStatusOgType(
    EtterlysningStatus status,
    EtterlysningType type
) {
}
