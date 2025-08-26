package no.nav.ung.kodeverk.formidling;

/**
 * Pdfgen mal filer
 */
public enum TemplateType {
    INNVILGELSE("innvilgelse", "ungdomsprogramytelse", "Førstegangsinnvilgelse"),
    ENDRING_INNTEKT("endring_inntekt", "ungdomsprogramytelse", "Endring av inntekt"),
    ENDRING_HØY_SATS("endring_høy_sats", "ungdomsprogramytelse", "Endring til høy sats"),
    ENDRING_BARNETILLEGG("endring_barnetillegg", "ungdomsprogramytelse", "Endring pga barnetillegg"),
    MANUELT_VEDTAKSBREV("manuelt_vedtaksbrev", "ungdomsprogramytelse", "Manuelt vedtaksbrev"),
    OPPHØR("opphør", "ungdomsprogramytelse", "Opphør av ungdomsprogramytelse"),
    ENDRING_PROGRAMPERIODE("endring_programperiode", "ungdomsprogramytelse","Endring av programperiode"),
    GENERELT_FRITEKSTBREV("generelt_fritekstbrev", "ungdomsprogramytelse", "Generelt fritekstbrev"),

    KLAGE_AVVIST("avvist", "klage", "Avvist klage grunnet formkrav"),
    KLAGE_MEDHOLD("medhold", "klage", "Medhold i klage - omgjøres"),
    KLAGE_OVERSENDT("oversendt", "klage", "Klage oversendt til Klageinstansen"),

    ;

    final String path;
    final String dir;
    final String beskrivelse;

    TemplateType(String path, String dir, String beskrivelse) {
        this.path = path;
        this.dir = dir;
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
