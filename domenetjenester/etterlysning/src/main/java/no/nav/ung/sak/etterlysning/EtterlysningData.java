package no.nav.ung.sak.etterlysning;

import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.time.LocalDateTime;
import java.util.UUID;

public record EtterlysningData(
    EtterlysningStatus status,
    LocalDateTime frist,
    UUID grunnlagsreferanse,
    DatoIntervallEntitet periode,
    LocalDateTime opprettetTidspunkt,
    UttalelseData uttalelseData
) {
    public static EtterlysningData utenUttalelse(
        EtterlysningStatus status,
        LocalDateTime frist,
        UUID grunnlagsreferanse,
        DatoIntervallEntitet periode,
        LocalDateTime opprettetTidspunkt) {
        return new EtterlysningData(status, frist, grunnlagsreferanse, periode, opprettetTidspunkt, null);
    }

}
