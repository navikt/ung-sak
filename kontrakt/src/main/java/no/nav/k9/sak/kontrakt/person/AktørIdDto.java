package no.nav.k9.sak.kontrakt.person;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.abac.AbacAttributt;
import no.nav.k9.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AktørIdDto {

    @JsonProperty("aktørId")
    @NotNull
    @Valid
    private AktørId aktørId;

    AktørIdDto() {
        //
    }

    public AktørIdDto(AktørId aktørId) {
        this.aktørId = aktørId;
    }

    @AbacAttributt(value = "aktorId", masker = true)
    public String getAktorId() {
        return aktørId.getId();
    }
    
    public AktørId getAktørId() {
        return aktørId;
    }
}
