package no.nav.k9.sak.kontrakt.hendelser;

import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class HendelseDto {

    @JsonProperty(value = "aktørId", required = true)
    @NotNull
    @Valid
    private AktørId aktørId;

    @JsonProperty(value = "hendelse", required = true)
    @NotNull
    @Valid
    private Hendelse hendelse;

    HendelseDto() {
    }

    @JsonCreator
    public HendelseDto(@JsonProperty(value = "hendelse", required = true) @NotNull Hendelse hendelse,
                       @JsonProperty(value = "aktørId", required = true) @NotNull AktørId aktørId) {
        this.hendelse = Objects.requireNonNull(hendelse, "hendelse");
        this.aktørId = Objects.requireNonNull(aktørId, "aktørId");
    }


    @AbacAttributt(value = "aktorId", masker = true)
    public String getAktorId() {
        return aktørId.getId();
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public Hendelse getHendelse() {
        return hendelse;
    }
}
