package no.nav.k9.sak.kontrakt.person;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.person.Diskresjonskode;
import no.nav.k9.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AktørIdOgFnrDto {


    @JsonAlias("aktørId")
    @JsonProperty(value = "aktoerId", required = true)
    @NotNull
    @Valid
    private AktørId aktørId;

    @JsonProperty(value = "fnr", required = true)
    @Size(max = 11)
    @Pattern(regexp = "^[\\p{Alnum}]{11}+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @NotNull
    private String fnr;


    public AktørIdOgFnrDto() {
    }

    public AktørId getAktørId() {
        return aktørId;
    }


    @AbacAttributt("fnr")
    public String getFnr() {
        return fnr;
    }

    public void setAktørId(AktørId aktoerId) {
        this.aktørId = aktoerId;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

}
