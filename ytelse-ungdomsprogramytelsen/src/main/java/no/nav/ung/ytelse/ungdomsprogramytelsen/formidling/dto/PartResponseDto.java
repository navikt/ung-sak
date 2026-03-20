package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto;

import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.RolleType;

public record PartResponseDto(
    String id,
    String navn,
    IdType type,
    RolleType rolleType
    ) {
}
