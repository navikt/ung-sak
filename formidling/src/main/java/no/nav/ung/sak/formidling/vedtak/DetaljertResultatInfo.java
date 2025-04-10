package no.nav.ung.sak.formidling.vedtak;

public record DetaljertResultatInfo(DetaljertResultatType detaljertResultatType, String forklaring) {

    public static DetaljertResultatInfo of(DetaljertResultatType detaljertResultatType) {
        return new DetaljertResultatInfo(detaljertResultatType, null);
    }

    public static DetaljertResultatInfo of(DetaljertResultatType detaljertResultatType, String beskrivelse) {
        return new DetaljertResultatInfo(detaljertResultatType, beskrivelse);
    }
}
