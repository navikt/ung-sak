package no.nav.ung.kodeverk.hjemmel;

/**
 * Forskrift om forsøk med aktivitetspenger for unge (FOR-2026-09-27-1944), hjemlet i folketrygdloven § 25-13.
 */
public final class AktivitetspengerForskrift {

    public static final String NAVN = "Forskrift om forsøk med aktivitetspenger for unge (forsøket Et enklere Nav), Trondheim kommune, Trøndelag";

    private AktivitetspengerForskrift() {
    }

    public static String paragraf(String paragraf) {
        return NAVN + " § " + paragraf;
    }
}
