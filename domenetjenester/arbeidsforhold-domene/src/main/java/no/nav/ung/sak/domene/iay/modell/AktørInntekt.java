package no.nav.ung.sak.domene.iay.modell;


import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.typer.AktørId;

import java.util.*;
import java.util.stream.Collectors;

public class AktørInntekt implements IndexKey {

    private AktørId aktørId;

    @ChangeTracked
    private Set<Inntekt> inntekt = new LinkedHashSet<>();

    AktørInntekt() {
    }

    /**
     * Deep copy ctor
     */
    AktørInntekt(AktørInntekt aktørInntekt) {
        this.aktørId = aktørInntekt.getAktørId();

        this.inntekt = aktørInntekt.inntekt.stream().map(i -> {
            var inntekt = new Inntekt(i);
            inntekt.setAktørInntekt(this);
            return inntekt;
        }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { getAktørId() };
        return IndexKeyComposer.createKey(keyParts);
    }

    /**
     * Aktøren inntekten er relevant for
     * @return aktørid
     */
    public AktørId getAktørId() {
        return aktørId;
    }

    void setAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    /** Get alle inntekter samlet (ufiltrert). */
    public Collection<Inntekt> getInntekt() {
        return List.copyOf(inntekt);
    }

    public boolean hasValues() {
        return aktørId != null || inntekt != null;
    }

    void leggTilInntekt(Inntekt inntekt) {
        this.inntekt.add(inntekt);
        inntekt.setAktørInntekt(this);
    }

    void fjernInntekterFraKilde(InntektsKilde inntektsKilde) {
        this.inntekt.removeIf(it -> it.getInntektsKilde().equals(inntektsKilde));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof AktørInntekt)) {
            return false;
        }
        AktørInntekt other = (AktørInntekt) obj;
        return Objects.equals(this.getAktørId(), other.getAktørId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            ", inntekt=" + inntekt +
            '>';
    }

}
