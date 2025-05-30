package no.nav.ung.sak.typer;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.api.IndexKey;

/** En arbeidsgiver (enten virksomhet eller personlig arbeidsgiver). */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class Arbeidsgiver implements IndexKey {

    /**
     * Kun en av denne og {@link #arbeidsgiverAktørId} kan være satt. Sett denne
     * hvis Arbeidsgiver er en Organisasjon.
     */
    @JsonProperty(value = "arbeidsgiverOrgnr")
    @Valid
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String arbeidsgiverOrgnr;

    /**
     * Kun en av denne og {@link #virksomhet} kan være satt. Sett denne hvis
     * Arbeidsgiver er en Enkelt person.
     */
    @JsonProperty(value = "arbeidsgiverAktørId")
    @Valid
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String arbeidsgiverAktørId;

    protected Arbeidsgiver() {
        // for JPA
    }

    @JsonCreator
    public Arbeidsgiver(
            @JsonProperty(value = "arbeidsgiverOrgnr") @Valid @Size(max = 20) @Pattern(regexp = "^\\d+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String arbeidsgiverOrgnr,
            @JsonProperty(value = "arbeidsgiverAktørId") @Valid @Size(max = 20) @Pattern(regexp = "^\\d+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") AktørId arbeidsgiverAktørId) {
        this.arbeidsgiverOrgnr = nonEmpty(arbeidsgiverOrgnr);
        this.arbeidsgiverAktørId = arbeidsgiverAktørId == null ? null : arbeidsgiverAktørId.getAktørId();

        if (this.arbeidsgiverAktørId == null && this.arbeidsgiverOrgnr == null) {
            throw new IllegalArgumentException("Utvikler-feil: arbeidsgiver uten hverken orgnr eller aktørId");
        } else if (this.arbeidsgiverAktørId != null && this.arbeidsgiverOrgnr != null) {
            throw new IllegalArgumentException("Utvikler-feil: arbeidsgiver med både orgnr og aktørId");
        }
    }

    private String nonEmpty(String str) {
        return str == null || str.trim().isEmpty() ? null : str.trim();
    }

    @Override
    public String getIndexKey() {
        return getAktørId() != null
                ? "arbeidsgiverAktørId-" + getAktørId()
                : "virksomhet-" + getOrgnr();
    }

    public static Arbeidsgiver virksomhet(String arbeidsgiverOrgnr) {
        return new Arbeidsgiver(arbeidsgiverOrgnr, null);
    }

    public static Arbeidsgiver virksomhet(OrgNummer arbeidsgiverOrgnr) {
        return new Arbeidsgiver(arbeidsgiverOrgnr.getId(), null);
    }

    public static Arbeidsgiver person(AktørId arbeidsgiverAktørId) {
        return new Arbeidsgiver(null, arbeidsgiverAktørId);
    }

    /**
     * Virksomhets orgnr. Leser bør ta høyde for at dette kan være juridisk orgnr
     * (istdf. virksomhets orgnr).
     */
    public String getOrgnr() {
        return arbeidsgiverOrgnr;
    }

    /** Hvis arbeidsgiver er en privatperson, returner aktørId for person. */
    public AktørId getAktørId() {
        return getArbeidsgiverAktørId() == null ? null : new AktørId(getArbeidsgiverAktørId());
    }

    public String getArbeidsgiverAktørId() {
        return arbeidsgiverAktørId;
    }

    public String getArbeidsgiverOrgnr() {
        return arbeidsgiverOrgnr;
    }

    /**
     * Returneer ident for arbeidsgiver. Kan være Org nummer eller Aktør id (dersom
     * arbeidsgiver er en enkelt person -
     * f.eks. for Frilans el.)
     */
    public String getIdentifikator() {
        if (arbeidsgiverAktørId != null) {
            return getAktørId().getId();
        }
        return getOrgnr();
    }

    /**
     * Return true hvis arbeidsgiver er en {@link Virksomhet}, false hvis en Person.
     */
    public boolean getErVirksomhet() {
        return getOrgnr() != null;
    }

    /**
     * Return true hvis arbeidsgiver er en {@link AktørId}, ellers false.
     */
    public boolean erAktørId() {
        return getAktørId() != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof Arbeidsgiver))
            return false;
        Arbeidsgiver that = (Arbeidsgiver) o;
        return Objects.equals(getOrgnr(), that.getOrgnr()) &&
                Objects.equals(getAktørId(), that.getAktørId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrgnr(), getAktørId());
    }

    @Override
    public String toString() {
        return toString(this);
    }

    private static String toString(Arbeidsgiver arb) {
        // litt maskering for feilsøking nå
        if (arb.getErVirksomhet()) {
            return "Virksomhet<" + masker(arb.getIdentifikator()) + ">";
        } else {
            return "PersonligArbeidsgiver<"
                    + arb.getIdentifikator().substring(0, Math.min(arb.getIdentifikator().length(), 3)) + "...>";
        }
    }

    private static String masker(String identifikator) {
        return identifikator.substring(0, Math.min(identifikator.length(), 3))
            + "...";
    }

    public static Arbeidsgiver fra(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null)
            return null;
        return new Arbeidsgiver(arbeidsgiver.getOrgnr(), arbeidsgiver.getAktørId());
    }

    public static Arbeidsgiver fra(AktørId aktørId) {
        return fra(person(aktørId));
    }
}
