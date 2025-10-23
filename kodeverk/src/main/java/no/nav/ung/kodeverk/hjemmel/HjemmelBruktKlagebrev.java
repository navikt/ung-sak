package no.nav.ung.kodeverk.hjemmel;

public enum HjemmelBruktKlagebrev {

    FL_VEDTAK_SOM_KAN_PÅKLAGES(Hjemmel.FORVALTNINGSLOVEN_PARAGRAF_28, "28"),
    FL_OVERSITTING_AV_KLAGEFRIST(Hjemmel.FORVALTNINGSLOVEN_PARAGRAF_31, "31"),
    FL_ADRESSAT_FORM_OG_INNHOLD(Hjemmel.FORVALTNINGSLOVEN_PARAGRAF_32, "32"),
    FL_SAKSFORBEREDELSE_I_KLAGESAK(Hjemmel.FORVALTNINGSLOVEN_PARAGRAF_33, "33"),
    ARBEIDSMARKEDSLOVEN_17(Hjemmel.ARBEIDSMARKEDSLOVEN_PARAGRAF_17, "17"),

    MANGLER(Hjemmel.UNG_FORSKRIFT_PARAGRAF_9, "-");

    private final Hjemmel hjemmel;
    private final String paragrafNummer;

    HjemmelBruktKlagebrev(Hjemmel hjemmel, String paragrafNummer) {
        this.hjemmel = hjemmel;
        this.paragrafNummer = paragrafNummer;
    }

    public Hjemmel getHjemmel() {
        return hjemmel;
    }

    public String getParagrafNummer() {
        return paragrafNummer;
    }
}
