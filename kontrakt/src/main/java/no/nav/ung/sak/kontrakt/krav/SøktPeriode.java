package no.nav.ung.sak.kontrakt.krav;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.kodeverk.uttak.UttakArbeidType;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;
import no.nav.ung.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SøktPeriode {

    @Valid
    @NotNull
    @JsonProperty("periode")
    private Periode periode;

    @Valid
    @JsonProperty("type")
    private UttakArbeidType type;

    @Valid
    @JsonProperty("arbeidsgiver")
    private Arbeidsgiver arbeidsgiver;

    @Valid
    @JsonProperty("arbeidsforholdRef")
    private InternArbeidsforholdRef arbeidsforholdRef;

    @JsonCreator
    public SøktPeriode(@Valid @NotNull @JsonProperty("periode") Periode periode,
                       @Valid @JsonProperty("type") UttakArbeidType type,
                       @Valid @JsonProperty("arbeidsgiver") Arbeidsgiver arbeidsgiver,
                       @Valid @JsonProperty("arbeidsforholdRef") InternArbeidsforholdRef arbeidsforholdRef) {
        this.periode = periode;
        this.type = type;
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
    }

    public Periode getPeriode() {
        return periode;
    }

    public UttakArbeidType getType() {
        return type;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }
}
