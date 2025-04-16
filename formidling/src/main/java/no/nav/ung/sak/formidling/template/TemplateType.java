package no.nav.ung.sak.formidling.template;

/**
 * Pdfgen mal filer
 */
public enum TemplateType {
    INNVILGELSE("innvilgelse"),
    ENDRING_INNTEKT("endring_inntekt"),
    ENDRING_HØY_SATS("endring_høy_sats"),
    MANUELL_VEDTAKSBREV("manuell_vedtaksbrev");

    final String path;
    final String dir = "ungdomsytelse";

    TemplateType(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getDir() {
        return dir;
    }
}
