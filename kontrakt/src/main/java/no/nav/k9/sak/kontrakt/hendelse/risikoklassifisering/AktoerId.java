package no.nav.k9.sak.kontrakt.hendelse.risikoklassifisering;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AktoerId {

    @JsonProperty(value = "aktoerId", required = true)
    @Valid
    @NotNull
    private AktørId aktoerId;

    public AktoerId(AktørId aktoerId) {
        this.aktoerId = aktoerId;
    }

    public AktørId getAktoerId() {
        return aktoerId;
    }

}
