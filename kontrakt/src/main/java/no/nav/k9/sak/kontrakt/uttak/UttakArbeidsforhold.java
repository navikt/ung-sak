package no.nav.k9.sak.kontrakt.uttak;

import java.util.Comparator;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.typer.AktørId;

@JsonPropertyOrder({ "type", "organisasjonsnummer", "aktørId", "arbeidsforholdId" })
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class UttakArbeidsforhold implements Comparable<UttakArbeidsforhold> {

    /**
     * Kun en av denne og {@link #aktørId} kan være satt. Sett denne hvis Arbeidsgiver er en Organisasjon.
     */
    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "organisasjonsnummer")
    @Valid
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String organisasjonsnummer;

    /**
     * Kun en av denne og {@link #organisasjonsnummer} kan være satt. Sett denne hvis Arbeidsgiver er en Enkelt Privatperson.
     */
    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "aktørId")
    @Valid
    private AktørId aktørId;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "type")
    @Valid
    private UttakArbeidType type;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "arbeidsforholdId")
    @Valid
    @Size(max = 40)
    @Pattern(regexp = "^[\\p{Alnum}_\\.\\-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String arbeidsforholdId;

    public UttakArbeidsforhold(@JsonProperty(value = "organisasjonsnummer") @Valid @Size(max = 20) @Pattern(regexp = "^\\d+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String organisasjonsnummer,
                               @JsonProperty(value = "aktørId") @Valid AktørId aktørId,
                               @JsonProperty(value = "type") @Valid UttakArbeidType type,
                               @JsonProperty(value = "arbeidsforholdId") @Valid @Size(max = 40) @Pattern(regexp = "^[\\p{Alnum}_\\.\\-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String arbeidsforholdId) {
        if (aktørId != null && organisasjonsnummer != null) {
            throw new IllegalArgumentException("Utvikler-feil: arbeidsgiver med både orgnr og aktørId");
        }
        this.organisasjonsnummer = organisasjonsnummer;
        this.aktørId = aktørId;
        this.type = type;
        this.arbeidsforholdId = arbeidsforholdId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof UttakArbeidsforhold))
            return false;
        var other = (UttakArbeidsforhold) obj;

        return Objects.equals(this.organisasjonsnummer, other.organisasjonsnummer)
            && Objects.equals(this.aktørId, other.aktørId)
            && Objects.equals(this.type, other.type)
            && Objects.equals(this.arbeidsforholdId, other.arbeidsforholdId);
    }

    public String getOrganisasjonsnummer() {
        return organisasjonsnummer;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public UttakArbeidType getType() {
        return type;
    }

    public String getArbeidsforholdId() {
        return arbeidsforholdId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(organisasjonsnummer, aktørId, type, arbeidsforholdId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
            + (organisasjonsnummer != null ? "organisasjonummer=" + organisasjonsnummer + ", " : "")
            + (aktørId != null ? "aktørId=" + aktørId : "")
            + (type != null ? ", type=" + type : "")
            + (arbeidsforholdId != null ? ", arbeidsforholdId=" + arbeidsforholdId : "")
            + ">";
    }

    @Override
    public int compareTo(UttakArbeidsforhold o) {
        return COMP.compare(this, o);
    }

    private static final Comparator<UttakArbeidsforhold> COMP = Comparator
        .comparing((UttakArbeidsforhold dto) -> dto.getType(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getOrganisasjonsnummer(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getAktørId(), Comparator.nullsFirst(Comparator.naturalOrder()))
        .thenComparing(dto -> dto.getArbeidsforholdId(), Comparator.nullsFirst(Comparator.naturalOrder()));

}
