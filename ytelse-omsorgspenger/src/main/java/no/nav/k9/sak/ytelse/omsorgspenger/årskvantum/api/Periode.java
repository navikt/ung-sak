package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api;

import com.fasterxml.jackson.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class Periode {

    @JsonValue
    @NotNull
    @Size(max = 10 + 1 + 10)
    @Pattern(regexp = "^[\\p{Alnum}:\\-/]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    @Valid
    public String iso8601Periode;

    @JsonIgnore
    public LocalDate fom;

    @JsonIgnore
    public LocalDate tom;

    @JsonCreator
    public Periode(String iso8601Periode) {
        String[] strings = iso8601Periode.split("/");
        this.fom = LocalDate.parse(strings[0]);
        this.tom = LocalDate.parse(strings[1]);
    }
}
