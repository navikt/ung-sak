package no.nav.ung.sak.behandlingslager.behandling.sporing;

public class IngenVerdi {

    private IngenVerdi() {
    }

    public static <T> IngenVerdi ingenVerdi(T ignorertVerdi) {
        return new IngenVerdi();
    }

    @Override
    public String toString() {
        return "IngenVerdi{}";
    }
}
