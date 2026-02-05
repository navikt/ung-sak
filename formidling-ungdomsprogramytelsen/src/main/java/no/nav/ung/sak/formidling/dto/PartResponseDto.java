package no.nav.ung.sak.formidling.dto;

import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.RolleType;

public record PartResponseDto(
    String id,
    String navn,
    IdType type,
    RolleType rolleType
    ) {
}
