package no.nav.k9.sak.kontrakt.hendelser;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.hendelser.HendelseType;
import no.nav.k9.sak.typer.AktørId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class HendelseDto {

    @JsonProperty(value = "hendelseType", required = true)
    @NotNull
    @Valid
    private HendelseType hendelseType;

    @JsonProperty(value = "aktørId", required = true)
    @NotNull
    @Valid
    private AktørId aktørId;

    @JsonProperty(value = "payload", required = true)
    @NotNull
    @Valid
    private String payload;


    @JsonCreator
    public HendelseDto(@JsonProperty(value = "hendelseType", required = true) HendelseType hendelseType,
                       @JsonProperty(value = "aktørId", required = true) @NotNull AktørId aktørId,
                       @JsonProperty(value = "payload", required = true) @NotNull String payload) {
        this.hendelseType = Objects.requireNonNull(hendelseType, "hendelseType");
        this.aktørId = Objects.requireNonNull(aktørId, "aktørId");
        this.payload = Objects.requireNonNull(payload, "payload");
    }

    public HendelseType getHendelseType() {
        return hendelseType;
    }

    @AbacAttributt(value = "aktorId", masker = true)
    public String getAktorId() {
        return aktørId.getId();
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public String getPayload() {
        return payload;
    }
}
