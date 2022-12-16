package no.nav.k9.sak.kontrakt.opplæringspenger;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.kontrakt.dokument.TekstValideringRegex;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ReisetidDto {

    @JsonProperty(value = "opplæringPeriode", required = true)
    @Valid
    @NotNull
    private Periode opplæringPeriode;

    @JsonProperty(value = "reisetidTil", required = true)
    @Valid
    @NotNull
    private List<ReisetidPeriodeDto> reisetidTil;

    @JsonProperty(value = "reisetidHjem", required = true)
    @Valid
    @NotNull
    private List<ReisetidPeriodeDto> reisetidHjem;

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = TekstValideringRegex.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    public ReisetidDto() {
    }

    public ReisetidDto(Periode opplæringPeriode, List<ReisetidPeriodeDto> reisetidTil, List<ReisetidPeriodeDto> reisetidHjem, String begrunnelse) {
        this.opplæringPeriode = opplæringPeriode;
        this.reisetidTil = reisetidTil;
        this.reisetidHjem = reisetidHjem;
        this.begrunnelse = begrunnelse;
    }

    public ReisetidDto(LocalDate opplæringFom, LocalDate opplæringTom, List<ReisetidPeriodeDto> reisetidTil, List<ReisetidPeriodeDto> reisetidHjem, String begrunnelse) {
        this.opplæringPeriode = new Periode(opplæringFom, opplæringTom);
        this.reisetidTil = reisetidTil;
        this.reisetidHjem = reisetidHjem;
        this.begrunnelse = begrunnelse;
    }

    public Periode getOpplæringPeriode() {
        return opplæringPeriode;
    }

    public List<ReisetidPeriodeDto> getReisetidTil() {
        return reisetidTil;
    }

    public List<ReisetidPeriodeDto> getReisetidHjem() {
        return reisetidHjem;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }
}
