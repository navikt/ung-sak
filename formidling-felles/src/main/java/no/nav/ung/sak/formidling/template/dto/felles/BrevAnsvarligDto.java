package no.nav.ung.sak.formidling.template.dto.felles;

public record BrevAnsvarligDto(
    boolean automatiskBehandlet,
    String saksbehandlerNavn,
    String beslutterNavn) {
}
