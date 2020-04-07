package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ÅrskvantumResterendeDager {

    @JsonProperty(value = "antallDager", required = true)
    @Valid
    @NotNull
    private Integer antallDager;

    public ÅrskvantumResterendeDager(@Valid @NotNull Integer antallDager) {
        this.antallDager = antallDager;
    }

    public Integer getAntallDager() {
        return antallDager;
    }

    public void setAntallDager(Integer antallDager) {
        this.antallDager = antallDager;
    }
}
