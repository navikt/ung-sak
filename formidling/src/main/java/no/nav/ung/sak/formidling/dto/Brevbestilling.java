package no.nav.ung.sak.formidling.dto;

import com.fasterxml.jackson.databind.JsonNode;

import no.nav.ung.kodeverk.dokument.DokumentMalType;

public record Brevbestilling(
    Long behandlingId,
    DokumentMalType malType,
    String saksnummer,
    PartRequestDto mottaker,
    JsonNode dokumentdata
) {
}
