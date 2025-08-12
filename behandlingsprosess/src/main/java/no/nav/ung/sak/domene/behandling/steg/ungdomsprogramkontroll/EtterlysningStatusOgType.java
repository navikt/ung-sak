package no.nav.ung.sak.domene.behandling.steg.ungdomsprogramkontroll;

import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;

public record EtterlysningStatusOgType(
    EtterlysningStatus status,
    EtterlysningType type
) {
}
