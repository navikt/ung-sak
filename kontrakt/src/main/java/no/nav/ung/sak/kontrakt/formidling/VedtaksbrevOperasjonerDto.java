package no.nav.ung.sak.kontrakt.formidling;

public record VedtaksbrevOperasjonerDto(
    boolean kanRedigere,
    boolean harRedigert,
    boolean kanHindre,
    boolean harHindret
    //TODO legg ved original html for redigering
    // og html som er redigert
    // eller putt i eget endepunkt
) {
}
