package no.nav.ung.sak.formidling.vedtak.regler;

public record VedtaksbrevEgenskaper(
    boolean kanHindre,
    boolean kanOverstyreHindre,
    boolean kanRedigere,
    boolean kanOverstyreRediger
) {
}
