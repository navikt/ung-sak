package no.nav.k9.sak.kontrakt.person;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.person.Diskresjonskode;
import no.nav.k9.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public abstract class PersonIdentDto {

    @JsonAlias("aktørId")
    @JsonProperty(value = "aktoerId", required = true)
    @NotNull
    @Valid
    private AktørId aktørId;

    @JsonProperty(value = "diskresjonskode")
    @Valid
    private Diskresjonskode diskresjonskode;

    @JsonProperty(value = "fnr")
    @Size(max = 11)
    @Pattern(regexp = "^[\\p{Alnum}]{11}+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String fnr;

    public PersonIdentDto() {
        //
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public Diskresjonskode getDiskresjonskode() {
        return diskresjonskode;
    }

    @AbacAttributt("fnr")
    public String getFnr() {
        return fnr;
    }

    public void setAktørId(AktørId aktoerId) {
        this.aktørId = aktoerId;
    }

    public void setDiskresjonskode(Diskresjonskode diskresjonskode) {
        this.diskresjonskode = diskresjonskode;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

}
