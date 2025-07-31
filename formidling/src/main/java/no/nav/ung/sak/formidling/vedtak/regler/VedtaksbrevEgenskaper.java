package no.nav.ung.sak.formidling.vedtak.regler;

public record VedtaksbrevEgenskaper(
    boolean harBrev,
    IngenBrevÅrsakType ingenBrevÅrsakType,
    boolean kanHindre,
    boolean kanOverstyreHindre,
    boolean kanRedigere,
    boolean kanOverstyreRediger
) {
}
