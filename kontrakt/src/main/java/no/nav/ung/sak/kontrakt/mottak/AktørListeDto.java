package no.nav.ung.sak.kontrakt.mottak;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = false)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AktørListeDto {

    @JsonProperty(value = "aktører", required = true)
    @NotNull
    @Valid
    @Size(max = 10000)
    private List<AktørId> aktører;

    public AktørListeDto() {
        // empty ctor
    }

    public AktørListeDto(List<AktørId> aktører) {
        this.aktører = aktører;
    }

    public List<AktørId> getAktører() {
        return aktører;
    }
}
