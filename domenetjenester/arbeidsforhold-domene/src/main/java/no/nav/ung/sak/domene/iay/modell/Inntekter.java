package no.nav.ung.sak.domene.iay.modell;


import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;

import java.util.*;
import java.util.stream.Collectors;

public class Inntekter {

    @ChangeTracked
    private Set<Inntekt> inntekt = new LinkedHashSet<>();

    Inntekter() {
    }

    /**
     * Deep copy ctor
     */
    Inntekter(Inntekter inntekter) {
        this.inntekt = inntekter.inntekt.stream().map(i -> {
            var inntekt = new Inntekt(i);
            inntekt.setInntekter(this);
            return inntekt;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Get alle inntekter samlet (ufiltrert). */
    public Collection<Inntekt> getInntekt() {
        return List.copyOf(inntekt);
    }

    public boolean hasValues() {
        return inntekt != null;
    }

    void leggTilInntekt(Inntekt inntekt) {
        this.inntekt.add(inntekt);
        inntekt.setInntekter(this);
    }

    void fjernInntekterFraKilde(InntektsKilde inntektsKilde) {
        this.inntekt.removeIf(it -> it.getInntektsKilde().equals(inntektsKilde));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof Inntekter)) {
            return false;
        }
        Inntekter other = (Inntekter) obj;
        return Objects.equals(inntekt, other.inntekt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inntekt);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            ", inntekt=" + inntekt +
            '>';
    }

}
