package no.nav.ung.sak.behandlingslager.behandling.klage;

import no.nav.ung.kodeverk.hjemmel.Hjemmel;

import static no.nav.ung.kodeverk.hjemmel.Hjemmel.*;

public enum HjemmelBruktKlagebrev {

    FL_VEDTAK_SOM_KAN_PÅKLAGES(FORVALTNINGSLOVEN_PARAGRAF_28, "28"),
    FL_OVERSITTING_AV_KLAGEFRIST(FORVALTNINGSLOVEN_PARAGRAF_31, "31"),
    FL_ADRESSAT_FORM_OG_INNHOLD(FORVALTNINGSLOVEN_PARAGRAF_32, "32"),
    FL_SAKSFORBEREDELSE_I_KLAGESAK(FORVALTNINGSLOVEN_PARAGRAF_33, "33"),
    ARBEIDSMARKEDSLOVEN_17(ARBEIDSMARKEDSLOVEN_PARAGRAF_17, "17");

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
