package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.gjennomgått;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.sak.typer.Periode;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OpplæringPeriodeDto {

    //TODO erstatt med Periode
    @JsonProperty(value = "opplæring", required = true)
    @Valid
    @NotNull
    private Periode periode;

    public OpplæringPeriodeDto(Periode periode) {
        this.periode = periode;
    }

    public Periode getPeriode() {
        return periode;
    }
}
