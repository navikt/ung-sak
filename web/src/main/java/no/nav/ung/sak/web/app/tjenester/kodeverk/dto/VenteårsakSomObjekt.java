package no.nav.ung.sak.web.app.tjenester.kodeverk.dto;

import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Denne eine Kodeverdi typen Venteårsak har eit ekstra felt som skal serialiserast ut til frontend. Må derfor deklarerast som eigen klasse.
 */
public class VenteårsakSomObjekt extends KodeverdiSomObjekt<Venteårsak> {
    @NotNull
    private final boolean kanVelges;

    public VenteårsakSomObjekt(final Venteårsak from) {
        super(from);
        this.kanVelges = from.getKanVelgesIGui();
    }

    public static SortedSet<VenteårsakSomObjekt> sorterteVenteårsaker(final Set<Venteårsak> verdier) {
        final SortedSet<VenteårsakSomObjekt> result = new TreeSet<>((a, b) -> a.getKode().compareTo(b.getKode()));
        result.addAll(verdier.stream().map(VenteårsakSomObjekt::new).toList());
        return result;
    }

    public boolean getKanVelges() {
        return this.kanVelges;
    }
}
