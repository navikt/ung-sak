package no.nav.ung.sak.kontrakt.beregninginput;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


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
    @Size()
    @JsonProperty("aktivitetliste")
    private List<OverstyrBeregningAktivitet> aktivitetliste;

    @Valid
    @JsonProperty("harKategoriNæring")
    private Boolean harKategoriNæring;

    @Valid
    @JsonProperty("harKategoriFrilans")
    private Boolean harKategoriFrilans;


    public OverstyrBeregningInputPeriode() {
    }

    public OverstyrBeregningInputPeriode(LocalDate skjaeringstidspunkt, List<OverstyrBeregningAktivitet> aktivitetliste) {
        this.skjaeringstidspunkt = skjaeringstidspunkt;
        this.aktivitetliste = aktivitetliste;
    }

    public OverstyrBeregningInputPeriode(LocalDate skjaeringstidspunkt, List<OverstyrBeregningAktivitet> aktivitetliste, Boolean harKategoriNæring, Boolean harKategoriFrilans) {
        this.skjaeringstidspunkt = skjaeringstidspunkt;
        this.aktivitetliste = aktivitetliste;
        this.harKategoriNæring = harKategoriNæring;
        this.harKategoriFrilans = harKategoriFrilans;
    }

    public LocalDate getSkjaeringstidspunkt() {
        return skjaeringstidspunkt;
    }

    public List<OverstyrBeregningAktivitet> getAktivitetliste() {
        return aktivitetliste;
    }

    public Boolean getHarKategoriNæring() {
        return harKategoriNæring;
    }

    public Boolean getHarKategoriFrilans() {
        return harKategoriFrilans;
    }

}
