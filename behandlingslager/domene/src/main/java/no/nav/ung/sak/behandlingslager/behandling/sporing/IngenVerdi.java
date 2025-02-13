package no.nav.ung.sak.behandlingslager.behandling.sporing;

public class IngenVerdi extends Sporingsverdi {

    private IngenVerdi() {
    }

    public static <T> IngenVerdi ingenVerdi(T ignorertVerdi) {
        return new IngenVerdi();
    }

    @Override
    public String tilRegelVerdi() {
        return "INGEN_VERDI";
    }

    @Override
    public String toString() {
        return "IngenVerdi{}";
    }
}
