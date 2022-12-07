package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.gjennomgått;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ReisetidVurderingDto {

    @JsonProperty(value = "reisetidTil", required = true)
    @Valid
    private Periode reisetidTil;

    @JsonProperty(value = "reisetidHjem", required = true)
    @Valid
    private Periode reisetidHjem;

    @JsonProperty(value = "begrunnelse", required = true)
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}§]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String begrunnelse;

    public ReisetidVurderingDto(Periode reisetidTil, Periode reisetidHjem, String begrunnelse) {
        this.reisetidTil = reisetidTil;
        this.reisetidHjem = reisetidHjem;
        this.begrunnelse = begrunnelse;
    }

    public Periode getReisetidTil() {
        return reisetidTil;
    }

    public Periode getReisetidHjem() {
        return reisetidHjem;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
