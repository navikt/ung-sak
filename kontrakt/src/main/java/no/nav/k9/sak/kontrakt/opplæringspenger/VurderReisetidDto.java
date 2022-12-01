package no.nav.k9.sak.kontrakt.opplæringspenger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.kontrakt.dokument.TekstValideringRegex;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class VurderReisetidDto {

    @JsonProperty(value = "reisetidTil", required = true)
    @Valid
    private Periode reisetidTil;

    @JsonProperty(value = "reisetidHjem", required = true)
    @Valid
    private Periode reisetidHjem;

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = TekstValideringRegex.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    public VurderReisetidDto() {
    }

    public VurderReisetidDto(Periode reisetidTil, Periode reisetidHjem, String begrunnelse) {
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
