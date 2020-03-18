package no.nav.k9.sak.kontrakt.person;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AktørIdDto {

    @JsonProperty("aktørId")
    @NotNull
    @Valid
    private String aktørId;

    @JsonCreator
    public AktørIdDto(@NotNull @Size(max = 20) @Pattern(regexp = "^\\d+$", message = "AktørId '${validatedValue}' matcher ikke tillatt pattern '{regexp}'") String aktørId) {
        this.aktørId = aktørId;
    }
    
    public AktørIdDto(AktørId aktørId) {
        this.aktørId = aktørId.getId();
    }

    @AbacAttributt(value = "aktorId", masker = true)
    public String getAktorId() {
        return aktørId;
    }

    public AktørId getAktørId() {
        return new AktørId(aktørId);
    }

}
