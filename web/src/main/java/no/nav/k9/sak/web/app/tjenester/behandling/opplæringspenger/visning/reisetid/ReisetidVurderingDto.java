package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.reisetid;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ReisetidVurderingDto {

    @JsonProperty(value = "opplæringPeriode", required = true)
    @Valid
    @NotNull
    private Periode opplæringPeriode;

    @JsonProperty(value = "reisetidTil", required = true)
    @Valid
    @Size(max = 100)
    private List<ReisetidPeriodeVurderingDto> reisetidTil;

    @JsonProperty(value = "reisetidHjem", required = true)
    @Valid
    @Size(max = 100)
    private List<ReisetidPeriodeVurderingDto> reisetidHjem;

    public ReisetidVurderingDto(Periode opplæringPeriode, List<ReisetidPeriodeVurderingDto> reisetidTil, List<ReisetidPeriodeVurderingDto> reisetidHjem) {
        this.opplæringPeriode = opplæringPeriode;
        this.reisetidTil = reisetidTil;
        this.reisetidHjem = reisetidHjem;
    }

    public Periode getOpplæringPeriode() {
        return opplæringPeriode;
    }

    public List<ReisetidPeriodeVurderingDto> getReisetidTil() {
        return reisetidTil;
    }

    public List<ReisetidPeriodeVurderingDto> getReisetidHjem() {
        return reisetidHjem;
    }
}
