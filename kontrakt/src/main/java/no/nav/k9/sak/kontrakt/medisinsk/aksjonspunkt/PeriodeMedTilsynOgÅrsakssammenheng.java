package no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PeriodeMedTilsynOgÅrsakssammenheng {

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    @JsonProperty("periode")
    @Valid
    private Periode periode;

    @JsonProperty(value = "årsaksammenhengBegrunnelse", required = true)
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String årsaksammenhengBegrunnelse;

    @JsonProperty(value = "årsaksammenheng", required = true)
    private Boolean årsaksammenheng;


    public PeriodeMedTilsynOgÅrsakssammenheng(@JsonProperty(value = "periode", required = true) @Valid Periode periode,
                                              @JsonProperty(value = "begrunnelse", required = true) @Size(max = 4000) @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String begrunnelse,
                                              @JsonProperty(value = "årsaksammenheng", required = true) Boolean årsaksammenheng, @JsonProperty(value = "årsaksammenhengBegrunnelse", required = true) @Size(max = 4000) @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]") String årsaksammenhengBegrunnelse) {
        this.periode = periode;
        this.begrunnelse = begrunnelse;
        this.årsaksammenhengBegrunnelse = årsaksammenhengBegrunnelse;
        this.årsaksammenheng = årsaksammenheng;
    }

    public String getÅrsaksammenhengBegrunnelse() {
        return årsaksammenhengBegrunnelse;
    }

    public Boolean getÅrsaksammenheng() {
        return årsaksammenheng;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Periode getPeriode() {
        return periode;
    }
}
