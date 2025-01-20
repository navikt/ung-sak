package no.nav.ung.sak.formidling.dto;

import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.RolleType;

public record PartRequestDto(
    String id,
    IdType type,
    RolleType rolleType
    ) {
}
