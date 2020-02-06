package no.nav.k9.sak.kontrakt.medisinsk;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PeriodeMedTilsyn {

    @JsonProperty("periode")
    @Valid
    private Periode periode;

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

    public PeriodeMedTilsyn() {
    }

    public PeriodeMedTilsyn(Periode periode, String begrunnelse) {
        this.periode = periode;
        this.begrunnelse = begrunnelse;
    }

    public Periode getPeriode() {
        return periode;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
