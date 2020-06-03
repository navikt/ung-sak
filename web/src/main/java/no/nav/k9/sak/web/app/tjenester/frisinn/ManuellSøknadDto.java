package no.nav.k9.sak.web.app.tjenester.frisinn;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.søknad.felles.Periode;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class ManuellSøknadDto {

    @JsonProperty(value = "fnr")
    @Valid
    @NotNull
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Size(min = 11, max = 11)
    private String fnr;

    @JsonProperty(value = "periode")
    @Valid
    @NotNull
    private Periode periode;


    public ManuellSøknadDto() {
    }

    public ManuellSøknadDto(@Valid @NotNull String fnr, @Valid @NotNull Periode periode) {
        this.fnr = fnr;
        this.periode = periode;
    }

    @AbacAttributt(value = "fnr", masker = true)
    public String getFnr() {
        return fnr;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }
}
