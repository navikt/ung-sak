package no.nav.k9.sak.domene.abakus;

import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;

public class MarkertInntektsmelding {
    private Inntektsmelding inntektsmelding;
    private DokumentStatus status;
    private String beskrivelse;

    public MarkertInntektsmelding(Inntektsmelding inntektsmelding, DokumentStatus status, String beskrivelse) {
        this.inntektsmelding = inntektsmelding;
        this.status = status;
        this.beskrivelse = beskrivelse;
    }

    public Inntektsmelding getInntektsmelding() {
        return inntektsmelding;
    }

    public DokumentStatus getStatus() {
        return status;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}
