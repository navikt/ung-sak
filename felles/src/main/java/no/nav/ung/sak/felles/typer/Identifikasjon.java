package no.nav.ung.sak.felles.typer;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Identifikasjon {

    @Valid
    @NotNull
    @Size(max=20)
    @Pattern(regexp = "^\\d+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    public final String id;

    @Valid
    @NotNull
    public final IdType type;

    @JsonCreator
    public Identifikasjon(@JsonProperty("id") String id,
                          @JsonProperty("type") IdType type) {
        this.id = id;
        this.type = type;
    }

    public static Identifikasjon av(AktørId aktørId) {
        return new Identifikasjon(aktørId.getId(), IdType.AKTØRID);
    }

    public static Identifikasjon av(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver.erAktørId()) {
            return new Identifikasjon(arbeidsgiver.getIdentifikator(), IdType.AKTØRID);
        } else
            return new Identifikasjon(arbeidsgiver.getIdentifikator(), IdType.ORGNR);
    }
}
