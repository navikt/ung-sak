package no.nav.k9.sak.kontrakt.medisinsk;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt.PeriodeMedTilsyn;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class PeriodeMedTilsynOgÅrsakssammenheng extends PeriodeMedTilsyn {

    @JsonProperty(value = "årsaksammenhengBegrunnelse", required = true)
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String årsaksammenhengBegrunnelse;

    @JsonProperty(value = "årsaksammenheng", required = true)
    private Boolean årsaksammenheng;


    public PeriodeMedTilsynOgÅrsakssammenheng(@JsonProperty(value = "periode", required = true) @Valid Periode periode,
                                              @JsonProperty(value = "begrunnelse", required = true) @Size(max = 4000) @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'") String begrunnelse,
                                              @JsonProperty(value = "årsaksammenheng", required = true) Boolean årsaksammenheng,
                                              @JsonProperty(value = "årsaksammenhengBegrunnelse", required = true) @Size(max = 4000) @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'") String årsaksammenhengBegrunnelse) {
        super(periode, begrunnelse);
        this.årsaksammenhengBegrunnelse = årsaksammenhengBegrunnelse;
        this.årsaksammenheng = årsaksammenheng;
    }

    public String getÅrsaksammenhengBegrunnelse() {
        return årsaksammenhengBegrunnelse;
    }

    public void setÅrsaksammenhengBegrunnelse(String årsaksammenhengBegrunnelse) {
        this.årsaksammenhengBegrunnelse = årsaksammenhengBegrunnelse;
    }

    public Boolean getÅrsaksammenheng() {
        return årsaksammenheng;
    }

    public void setÅrsaksammenheng(Boolean årsaksammenheng) {
        this.årsaksammenheng = årsaksammenheng;
    }
}
