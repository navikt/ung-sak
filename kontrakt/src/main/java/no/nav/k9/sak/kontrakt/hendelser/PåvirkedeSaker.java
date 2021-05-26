package no.nav.k9.sak.kontrakt.hendelser;

import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.behandling.SaksnummerDto;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PåvirkedeSaker {

    @Valid
    @Size(max = 100)
    @JsonProperty("saksnummere")
    private List<SaksnummerDto> saksnummere = Collections.emptyList();

    public PåvirkedeSaker() {
        //
    }

    @JsonCreator
    public PåvirkedeSaker(@JsonProperty(value = "saksnummere", required = true) @NotNull @Valid List<SaksnummerDto> saksnummere) {
        this.saksnummere = saksnummere;
    }

    public List<SaksnummerDto> getSaksnummere() {
        return saksnummere;
    }
}
