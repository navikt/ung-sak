package no.nav.k9.sak.kontrakt.uttak;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Årskvantum {

    @JsonProperty(value = "år", required = true)
    @Valid
    @NotNull
    private String år;

    @JsonProperty(value = "personIdent", required = true)
    @Valid
    @NotNull
    private String personIdent;

    @JsonProperty(value = "grunnrett", required = true)
    @Valid
    @NotNull
    private Integer grunnrett;

    @JsonProperty(value = "årskvantumRest", required = true)
    @Valid
    @NotNull
    private BigDecimal årskvantumRest;

    public String getÅr() {
        return år;
    }

    public void setÅr(String år) {
        this.år = år;
    }

    public String getPersonIdent() {
        return personIdent;
    }

    public void setPersonIdent(String personIdent) {
        this.personIdent = personIdent;
    }

    public Integer getGrunnrett() {
        return grunnrett;
    }

    public void setGrunnrett(Integer grunnrett) {
        this.grunnrett = grunnrett;
    }

    public BigDecimal getÅrskvantumRest() {
        return årskvantumRest;
    }

    public void setÅrskvantumRest(BigDecimal årskvantumRest) {
        this.årskvantumRest = årskvantumRest;
    }
}
