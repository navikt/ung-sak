package no.nav.ung.sak.kontrakt.aktivitetspenger;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;

/**
 *   Periode som også tillater å ha en åpen ende. Brukes i tilfeller der frontend ikke skal måtte forholde seg til sluttdato (som opphør)
 */
@JsonPropertyOrder({ "fom", "tom" })
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class ÅpenPeriode extends Periode {

    @JsonProperty(value = "fom", required = true)
    @Valid
    private LocalDate fom;

    @JsonProperty(value = "tom")
    @Valid
    private LocalDate tom;

    @JsonCreator
    public ÅpenPeriode(@JsonProperty("fom") LocalDate fom, @JsonProperty("tom") LocalDate tom) {
        super(fom, tom);
        this.fom = fom;
        this.tom = tom;
    }
}
