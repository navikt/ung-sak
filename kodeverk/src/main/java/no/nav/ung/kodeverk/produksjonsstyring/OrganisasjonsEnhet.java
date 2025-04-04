package no.nav.ung.kodeverk.produksjonsstyring;

import java.util.Objects;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OrganisasjonsEnhet {

    @JsonProperty(value = "enhetId")
    @Pattern(regexp = "^[\\p{L}\\p{N}]+$")
    @Size(min = 1, max = 30)
    private String enhetId;

    @JsonProperty(value = "enhetNavn")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\p{P}\\p{Space}]+$")
    @Size(min = 1, max = 10)
    private String enhetNavn;

    @JsonProperty(value = "status")
    @Pattern(regexp = "^[A-Z_]+$")
    @Size(min = 1, max = 50)
    private String status;

    protected OrganisasjonsEnhet() {
        //
    }

    public OrganisasjonsEnhet(String enhetId, String enhetNavn) {
        this.enhetId = enhetId;
        this.enhetNavn = enhetNavn;
    }

    public OrganisasjonsEnhet(String enhetId, String enhetNavn, String status) {
        this.enhetId = enhetId;
        this.enhetNavn = enhetNavn;
        this.status = status;
    }

    public String getEnhetId() {
        return enhetId;
    }

    public String getEnhetNavn() {
        return enhetNavn;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var other = (OrganisasjonsEnhet) obj;
        return Objects.equals(enhetId, other.enhetId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enhetId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<enhetId=" + enhetId + ", navn=" + enhetNavn + ", status=" + status + ">";
    }

}
