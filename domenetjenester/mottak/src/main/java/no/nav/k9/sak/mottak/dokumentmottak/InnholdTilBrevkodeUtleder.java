package no.nav.k9.sak.mottak.dokumentmottak;

import no.nav.k9.kodeverk.dokument.Brevkode;

public class InnholdTilBrevkodeUtleder {

    public static Brevkode utledForventetBrevkode(String dokumentinnhold) {
        if (erXml(dokumentinnhold)) {
            return Brevkode.INNTEKTSMELDING;
        }
        if (erJson(dokumentinnhold)) {
            return Brevkode.SØKNAD_UTBETALING_OMS;
        }
        return null;
    }

    private static boolean erXml(String payload) {
        return payload != null && payload.substring(0, Math.min(50, payload.length())).trim().startsWith("<");
    }

    private static boolean erJson(String payload) {
        return payload != null && payload.substring(0, Math.min(50, payload.length())).trim().startsWith("{");
    }

}

