package no.nav.k9.sak.typer;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.api.IndexKey;

/** En arbeidsgiver (enten virksomhet eller personlig arbeidsgiver). */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class Arbeidsgiver implements IndexKey {

    /**
     * Kun en av denne og {@link #arbeidsgiverAktørId} kan være satt. Sett denne hvis Arbeidsgiver er en Organisasjon.
     */
    @JsonProperty(value = "arbeidsgiverOrgnr")
    @Valid
    @Size(max=20)
    @Pattern(regexp = "^\\d+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String arbeidsgiverOrgnr;

    /**
     * Kun en av denne og {@link #virksomhet} kan være satt. Sett denne hvis Arbeidsgiver er en Enkelt person.
     */
    @JsonProperty(value = "arbeidsgiverAktørId")
    @Valid
    private AktørId arbeidsgiverAktørId;

    protected Arbeidsgiver() {
        // for JPA
    }

    protected Arbeidsgiver(String arbeidsgiverOrgnr, AktørId arbeidsgiverAktørId) {
        this.arbeidsgiverOrgnr = nonEmpty(arbeidsgiverOrgnr);
        this.arbeidsgiverAktørId = arbeidsgiverAktørId;
        
        if (this.arbeidsgiverAktørId == null && this.arbeidsgiverOrgnr == null) {
            throw new IllegalArgumentException("Utvikler-feil: arbeidsgiver uten hverken orgnr eller aktørId");
        } else if (this.arbeidsgiverAktørId != null && this.arbeidsgiverOrgnr != null) {
            throw new IllegalArgumentException("Utvikler-feil: arbeidsgiver med både orgnr og aktørId");
        }
    }

    private String nonEmpty(String str) {
        return str==null || str.trim().isEmpty()?null: str.trim();
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

    /** Virksomhets orgnr. Leser bør ta høyde for at dette kan være juridisk orgnr (istdf. virksomhets orgnr). */
    public String getOrgnr() {
        return arbeidsgiverOrgnr;
    }

    /** Hvis arbeidsgiver er en privatperson, returner aktørId for person. */
    public AktørId getAktørId() {
        return arbeidsgiverAktørId;
    }

    /**
     * Returneer ident for arbeidsgiver. Kan være Org nummer eller Aktør id (dersom arbeidsgiver er en enkelt person -
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
        return "Arbeidsgiver{" +
            "virksomhet=" + getOrgnr() +
            ", arbeidsgiverAktørId='" + getAktørId() + '\'' +
            '}';
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
