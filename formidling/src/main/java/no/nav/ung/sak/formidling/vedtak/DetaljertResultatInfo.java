package no.nav.ung.sak.formidling.vedtak;

public record DetaljertResultatInfo(DetaljertResultatType detaljertResultatType, String forklaring) {

    public static DetaljertResultatInfo of(DetaljertResultatType detaljertResultatType) {
        return new DetaljertResultatInfo(detaljertResultatType, null);
    }

    public static DetaljertResultatInfo of(DetaljertResultatType detaljertResultatType, String beskrivelse) {
        return new DetaljertResultatInfo(detaljertResultatType, beskrivelse);
    }

    public String utledForklaring() {
        var typeTekst = detaljertResultatType.name() + " - " + detaljertResultatType.getNavn();
        return forklaring != null && !forklaring.isBlank() ? typeTekst + ". Forklaring: " + forklaring : typeTekst;
    }
}
