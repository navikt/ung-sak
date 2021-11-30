package no.nav.k9.sak.kontrakt.beregninginput;

import java.time.LocalDate;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.uttak.Periode;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OverstyrBeregningInputPeriode {

    @Valid
    @NotNull
    @JsonProperty("skjaeringstidspunkt")
    private LocalDate skjaeringstidspunkt;

    @Valid
    @NotNull
    @Size(min = 1)
    @JsonProperty("aktivitetliste")
    private List<OverstyrBeregningAktivitet> aktivitetliste;

    public OverstyrBeregningInputPeriode() {
    }

    public OverstyrBeregningInputPeriode(LocalDate skjaeringstidspunkt, List<OverstyrBeregningAktivitet> aktivitetliste) {
        this.skjaeringstidspunkt = skjaeringstidspunkt;
        this.aktivitetliste = aktivitetliste;
    }

    public LocalDate getSkjaeringstidspunkt() {
        return skjaeringstidspunkt;
    }

    public List<OverstyrBeregningAktivitet> getAktivitetliste() {
        return aktivitetliste;
    }
}
