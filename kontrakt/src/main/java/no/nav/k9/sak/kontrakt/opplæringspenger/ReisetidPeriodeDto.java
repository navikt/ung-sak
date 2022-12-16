package no.nav.k9.sak.kontrakt.opplæringspenger;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ReisetidPeriodeDto {

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    private Periode periode;

    @JsonProperty(value = "godkjent", required = true)
    @NotNull
    private boolean godkjent;

    public ReisetidPeriodeDto() {
    }

    public ReisetidPeriodeDto(Periode periode, boolean godkjent) {
        this.periode = periode;
        this.godkjent = godkjent;
    }

    public ReisetidPeriodeDto(LocalDate fom, LocalDate tom, boolean godkjent) {
        this.periode = new Periode(fom, tom);
        this.godkjent = godkjent;
    }

    public Periode getPeriode() {
        return periode;
    }

    public boolean isGodkjent() {
        return godkjent;
    }
}
