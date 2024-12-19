package no.nav.ung.sak.formidling.dto;

import com.fasterxml.jackson.databind.JsonNode;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.typer.Saksnummer;

public record Brevbestilling(
    Long behandlingId,
    DokumentMalType malType,
    Saksnummer saksnummer,
    PartRequestDto mottaker,
    JsonNode dokumentdata
) {
}
