package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto;

import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.RolleType;

public record PartRequestDto(
    String id,
    IdType type,
    RolleType rolleType
    ) {
}
