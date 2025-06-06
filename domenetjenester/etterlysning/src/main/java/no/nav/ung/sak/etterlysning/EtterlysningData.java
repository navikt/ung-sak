package no.nav.ung.sak.etterlysning;

import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record EtterlysningData(
    EtterlysningStatus status,
    LocalDateTime frist,
    UUID grunnlagsreferanse,
    UttalelseData uttalelseData
) {


}
