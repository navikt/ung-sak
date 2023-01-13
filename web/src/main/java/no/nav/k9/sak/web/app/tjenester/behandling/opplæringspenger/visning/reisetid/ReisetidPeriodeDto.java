package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.reisetid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ReisetidPeriodeDto {

    @JsonProperty(value = "opplæringPeriode", required = true)
    @Valid
    @NotNull
    private Periode opplæringPeriode;

    @JsonProperty(value = "reisetidTil", required = true)
    @Valid
    private Periode reisetidTil;

    @JsonProperty(value = "reisetidHjem", required = true)
    @Valid
    private Periode reisetidHjem;

    public ReisetidPeriodeDto(Periode opplæringPeriode, Periode reisetidTil, Periode reisetidHjem) {
        this.opplæringPeriode = opplæringPeriode;
        this.reisetidTil = reisetidTil;
        this.reisetidHjem = reisetidHjem;
    }

    public Periode getOpplæringPeriode() {
        return opplæringPeriode;
    }

    public Periode getReisetidTil() {
        return reisetidTil;
    }

    public Periode getReisetidHjem() {
        return reisetidHjem;
    }
}
