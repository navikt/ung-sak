package no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap;

public enum MedlemskapAvslagsÅrsakType {
    SØKER_IKKE_MEDLEM("Søker har ikke bodd i et land med trygdeavtale siste 5 år.");

    private final String beskrivelse;

    MedlemskapAvslagsÅrsakType(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}
