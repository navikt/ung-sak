package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class PermisjonDto {

    @JsonProperty(value = "permisjonFom")
    @NotNull
    private LocalDate permisjonFom;

    @JsonProperty(value = "permisjonsprosent")
    @DecimalMin("0.00")
    @DecimalMax("200.00")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal permisjonsprosent;

    @JsonProperty(value = "permisjonTom")
    @NotNull
    private LocalDate permisjonTom;

    @JsonProperty(value = "type")
    @Valid
    @NotNull
    private PermisjonsbeskrivelseType type;

    public PermisjonDto(LocalDate permisjonFom, LocalDate permisjonTom, BigDecimal permisjonsprosent, PermisjonsbeskrivelseType type) {
        this.permisjonFom = permisjonFom;
        this.permisjonTom = permisjonTom;
        this.permisjonsprosent = permisjonsprosent;
        this.type = type;
    }

    PermisjonDto() {
        // Skjul private constructor
    }

    public LocalDate getPermisjonFom() {
        return permisjonFom;
    }

    public BigDecimal getPermisjonsprosent() {
        return permisjonsprosent;
    }

    public LocalDate getPermisjonTom() {
        return permisjonTom;
    }

    public PermisjonsbeskrivelseType getType() {
        return type;
    }

    public void setPermisjonFom(LocalDate permisjonFom) {
        this.permisjonFom = permisjonFom;
    }

    public void setPermisjonsprosent(BigDecimal permisjonsprosent) {
        this.permisjonsprosent = permisjonsprosent;
    }

    public void setPermisjonTom(LocalDate permisjonTom) {
        this.permisjonTom = permisjonTom;
    }

    public void setType(PermisjonsbeskrivelseType type) {
        this.type = type;
    }

}
