package no.nav.ung.kodeverk.formidling;

/**
 * Pdfgen mal filer
 */
public enum TemplateType {
    INNVILGELSE("innvilgelse", "Førstegangsinnvilgelse"),
    ENDRING_INNTEKT("endring_inntekt", "Endring av inntekt"),
    ENDRING_HØY_SATS("endring_høy_sats", "Endring til høy sats"),
    ENDRING_BARNETILLEGG("endring_barnetillegg", "Endring pga barnetillegg"),
    MANUELT_VEDTAKSBREV("manuelt_vedtaksbrev", "Manuelt vedtaksbrev"),
    TOM_VEDTAKSBREV_MAL("tom_vedtaksbrev_mal", "Tom vedtaksbrev mal for redigering"),
    OPPHØR("opphør", "Opphør av ungdomsprogramytelse"),
    ENDRING_PROGRAMPERIODE("endring_programperiode", "Endring av programperiode"),
    GENERELT_FRITEKSTBREV("generelt_fritekstbrev", "Generelt fritekstbrev"),;

    final String path;
    final String dir = "ungdomsprogramytelse";
    final String beskrivelse;

    TemplateType(String path, String beskrivelse) {
        this.path = path;
        this.beskrivelse = beskrivelse;
    }

    public String getPath() {
        return path;
    }

    public String getDir() {
        return dir;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}
