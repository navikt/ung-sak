package no.nav.ung.sak.formidling.vedtak.regler;

public record VedtaksbrevEgenskaper(
    boolean kanHindre,
    boolean kanOverstyreHindre,
    boolean kanRedigere,
    boolean kanOverstyreRediger
) {
    public static VedtaksbrevEgenskaper kanRedigere(boolean kanRedigere) {
        return new VedtaksbrevEgenskaper(kanRedigere, kanRedigere, kanRedigere, kanRedigere);
    }
}
