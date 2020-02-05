package no.nav.k9.sak.kontrakt.arbeidsforhold;


import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.Valid;
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

    @JsonProperty(value="permisjonFom")
    @NotNull
    private LocalDate permisjonFom;
    
    @JsonProperty(value="permisjonTom")
    @NotNull
    private LocalDate permisjonTom;
    
    @JsonProperty(value = "permisjonsprosent")
    private BigDecimal permisjonsprosent;
    
    @JsonProperty(value="type")
    @Valid
    @NotNull
    private PermisjonsbeskrivelseType type;

    PermisjonDto(){
        // Skjul private constructor
    }

    public PermisjonDto(LocalDate permisjonFom, LocalDate permisjonTom, BigDecimal permisjonsprosent, PermisjonsbeskrivelseType type) {
        this.permisjonFom = permisjonFom;
        this.permisjonTom = permisjonTom;
        this.permisjonsprosent = permisjonsprosent;
        this.type = type;
    }

    public LocalDate getPermisjonFom() {
        return permisjonFom;
    }

    public void setPermisjonFom(LocalDate permisjonFom) {
        this.permisjonFom = permisjonFom;
    }

    public LocalDate getPermisjonTom() {
        return permisjonTom;
    }

    public void setPermisjonTom(LocalDate permisjonTom) {
        this.permisjonTom = permisjonTom;
    }

    public BigDecimal getPermisjonsprosent() {
        return permisjonsprosent;
    }

    public void setPermisjonsprosent(BigDecimal permisjonsprosent) {
        this.permisjonsprosent = permisjonsprosent;
    }

    public PermisjonsbeskrivelseType getType() {
        return type;
    }

    public void setType(PermisjonsbeskrivelseType type) {
        this.type = type;
    }

}
