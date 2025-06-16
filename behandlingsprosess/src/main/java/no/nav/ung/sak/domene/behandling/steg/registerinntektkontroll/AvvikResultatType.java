package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

public enum AvvikResultatType {
    AVVIK_MED_REGISTERINNTEKT(1),
    AVVIK_UTEN_REGISTERINNTEKT(2),
    INGEN_AVVIK(3);


    private int prioritet;

    AvvikResultatType(int prioritet) {
        this.prioritet = prioritet;
    }

    public int getPrioritet() {
        return prioritet;
    }
}
