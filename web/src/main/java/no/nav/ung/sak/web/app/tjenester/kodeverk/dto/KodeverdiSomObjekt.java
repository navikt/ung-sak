package no.nav.ung.sak.web.app.tjenester.kodeverk.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import no.nav.ung.kodeverk.api.Kodeverdi;

import java.util.*;

/**
 * Blir brukt til å returnere full objektinfo om Kodeverdi enum type som ellers blir returnert som string med verdi lik kode.
 * Frontend bruker denne når den trenger å få full objektinfo slik at den kan gjere oppslag for å finne namn og kodeverk
 * type tilhøyrande ein kodeverdi enum som ellers blir representert som ein enkel string.
 */
public class KodeverdiSomObjekt<K extends Kodeverdi> {
    @NotNull
    private final String kode;
    @NotNull
    private final String kodeverk;
    @NotNull
    private final String navn;

    @NotNull
    @JsonIgnore
    private final K madeFrom;

    public KodeverdiSomObjekt(final K from) {
        this.kode = from.getKode();
        this.kodeverk = from.getKodeverk();
        this.navn = from.getNavn();
        this.madeFrom = from;
    }

    public K getMadeFrom() {
        return this.madeFrom;
    }

    public String madeFromClassName() {
        return this.getMadeFrom().getClass().getSimpleName();
    }

    public static <KV extends Kodeverdi> SortedSet<KodeverdiSomObjekt<KV>> sorterte(Set<KV> verdier) {
        final SortedSet<KodeverdiSomObjekt<KV>> result = new TreeSet<>((a, b) -> a.getKode().compareTo(b.getKode()));
        result.addAll(verdier.stream().map(KodeverdiSomObjekt::new).toList());
        return result;
    }

    public String getKode() {
        return kode;
    }

    public String getKodeverk() {
        return kodeverk;
    }

    public String getNavn() {
        return navn;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof KodeverdiSomObjekt<?> that)) return false;
        return Objects.equals(kode, that.kode) && Objects.equals(kodeverk, that.kodeverk) && Objects.equals(navn, that.navn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kode, kodeverk, navn);
    }
}
