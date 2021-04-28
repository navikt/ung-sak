package no.nav.k9.sak.kontrakt.omsorg;

public enum BarnRelasjon {
    MOR("Mor"),
    MEDMOR("Medmor"),
    FAR("Far"),
    FOSTERFORELDER("Fosterforelder"),
    ANNET("Annet");

    private final String rolle;

    BarnRelasjon(String rolle) {
        this.rolle = rolle;
    }

    public String getRolle() {
        return this.rolle;
    }

    public static BarnRelasjon of(String rolle) {
        if (rolle == null) {
            return null;
        }
        for (BarnRelasjon relasjon : BarnRelasjon.values()) {
            if (relasjon.getRolle().equals(rolle)) {
                return relasjon;
            }
        }
        return  BarnRelasjon.ANNET;

    }

}
