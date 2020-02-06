package no.nav.k9.sak.typer;

import java.io.Serializable;
import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.k9.kodeverk.api.IndexKey;

/**
 * Ekstern arbeidsforhold referanse.
 * Mottatt fra inntektsmelding eller AARegisteret.
 *
 * Hvis null gjelder det flere arbeidsforhold, ellers for et spesifikt forhold
 */
@JsonFormat(shape = JsonFormat.Shape.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class EksternArbeidsforholdRef implements IndexKey, Serializable {

    /** Representerer alle arbeidsforhold for en arbeidsgiver. */
    private static final EksternArbeidsforholdRef NULL_OBJECT = new EksternArbeidsforholdRef(null);

    @JsonValue
    @NotNull
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message="'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String referanse;

    EksternArbeidsforholdRef() {
    }

    @JsonCreator
    protected EksternArbeidsforholdRef(String referanse) {
        this.referanse = referanse;
    }

    public static EksternArbeidsforholdRef ref(String referanse) {
        return new EksternArbeidsforholdRef(referanse);
    }

    public static EksternArbeidsforholdRef nullRef() {
        return NULL_OBJECT;
    }

    public String getReferanse() {
        return referanse;
    }

    @Override
    public String getIndexKey() {
        return referanse;
    }

    public boolean gjelderForSpesifiktArbeidsforhold() {
        return referanse != null && !referanse.isEmpty();
    }

    public boolean gjelderFor(EksternArbeidsforholdRef ref) {
        Objects.requireNonNull(ref, "Forventer EksternArbeidsforholdRef.ref(null)");
        if (!gjelderForSpesifiktArbeidsforhold() || !ref.gjelderForSpesifiktArbeidsforhold()) {
            return true;
        }
        return Objects.equals(referanse, ref.referanse);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null && this.referanse == null) {
            return true;
        }
        if (o == null || getClass() != o.getClass())
            return false;
        EksternArbeidsforholdRef that = (EksternArbeidsforholdRef) o;
        return Objects.equals(referanse, that.referanse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referanse);
    }

    @Override
    public String toString() {
        return referanse;
    }
}
