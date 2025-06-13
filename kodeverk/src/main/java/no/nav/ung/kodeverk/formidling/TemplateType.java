package no.nav.ung.kodeverk.formidling;

/**
 * Pdfgen mal filer
 */
public enum TemplateType {
    INNVILGELSE("innvilgelse"),
    ENDRING_INNTEKT("endring_inntekt"),
    ENDRING_HØY_SATS("endring_høy_sats"),
    ENDRING_BARNETILLEGG("endring_barnetillegg"),
    MANUELL_VEDTAKSBREV("manuell_vedtaksbrev"),
    OPPHØR("opphør"),
    ENDRING_PROGRAMPERIODE("endring_programperiode"),
    GENERELT_FRITEKSTBREV("generelt_fritekstbrev");

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
