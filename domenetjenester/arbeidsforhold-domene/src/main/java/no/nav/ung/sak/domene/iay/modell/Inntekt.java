package no.nav.ung.sak.domene.iay.modell;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.typer.Arbeidsgiver;

public class Inntekt implements IndexKey {

    private Inntekter inntekter;

    private Arbeidsgiver arbeidsgiver;

    private InntektsKilde inntektsKilde;

    /* TODO: Bør InntektspostEntitet splittes? inneholder litt forskjellig felter avhengig av kilde. */
    @ChangeTracked
    private Set<Inntektspost> inntektspost = new LinkedHashSet<>();

    Inntekt() {
    }

    /**
     * Copy ctor
     */
    Inntekt(Inntekt inntektMal) {
        this.inntektsKilde = inntektMal.getInntektsKilde();
        this.arbeidsgiver = inntektMal.getArbeidsgiver();
        this.inntektspost = inntektMal.getAlleInntektsposter().stream().map(Inntektspost::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { getArbeidsgiver(), getInntektsKilde() };
        return IndexKeyComposer.createKey(keyParts);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null || !(obj instanceof Inntekt)) {
            return false;
        }
        Inntekt other = (Inntekt) obj;
        return Objects.equals(this.getInntektsKilde(), other.getInntektsKilde())
            && Objects.equals(this.getArbeidsgiver(), other.getArbeidsgiver());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getInntektsKilde(), getArbeidsgiver());
    }

    /**
     * System (+ filter) som inntektene er hentet inn fra / med
     *
     * @return {@link InntektsKilde}
     */
    public InntektsKilde getInntektsKilde() {
        return inntektsKilde;
    }

    void setInntektsKilde(InntektsKilde inntektsKilde) {
        this.inntektsKilde = inntektsKilde;
    }


    /**
     * Utbetaler
     *
     * @return {@link Arbeidsgiver}
     */
    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    void setArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    /**
     * Alle utbetalinger utført av utbetaler (ufiltrert).
     */
    public Collection<Inntektspost> getAlleInntektsposter() {
        return Collections.unmodifiableSet(inntektspost);
    }

    void leggTilInntektspost(Inntektspost inntektspost) {
        this.inntektspost.add(inntektspost);
    }

    void setInntekter(Inntekter inntekter) {
        this.inntekter = inntekter;
    }

    public InntektspostBuilder getInntektspostBuilder() {
        return InntektspostBuilder.ny();
    }

    public boolean hasValues() {
        return arbeidsgiver != null || inntektsKilde != null || inntektspost != null;
    }

}
