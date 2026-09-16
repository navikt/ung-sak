package no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap;

public enum MedlemskapAvslagsÅrsakType {
    SØKER_IKKE_MEDLEM("Søker oppfyller ikke krav om forutgående medlemskap.");

    private final String beskrivelse;

    MedlemskapAvslagsÅrsakType(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}
