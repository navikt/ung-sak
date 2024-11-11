package no.nav.ung.sak.kontrakt;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class ProsessTaskGruppeIdDto {

    @JsonProperty(value = "gruppe")
    @Size(min = 1, max = 250)
    @Pattern(regexp = "[a-zA-Z0-9-.]+")
    private String gruppe;

    public ProsessTaskGruppeIdDto(String gruppe) {
        this.gruppe = gruppe;
    }

    protected ProsessTaskGruppeIdDto() {
        //
    }

    public String getGruppe() {
        return gruppe;
    }

    @Override
    public String toString() {
        return "BehandlingIdDto{" +
            "behandlingId=" + gruppe +
            '}';
    }
}
