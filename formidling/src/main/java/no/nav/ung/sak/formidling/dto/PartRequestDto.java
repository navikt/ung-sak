package no.nav.ung.sak.formidling.dto;

import no.nav.ung.sak.formidling.kodeverk.IdType;
import no.nav.ung.sak.formidling.kodeverk.RolleType;

public record PartRequestDto(
    String id,
    IdType type,
    RolleType rolleType
    ) {
}
