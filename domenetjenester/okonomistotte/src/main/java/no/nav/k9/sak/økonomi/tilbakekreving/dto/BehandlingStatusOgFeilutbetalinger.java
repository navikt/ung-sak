package no.nav.k9.sak.økonomi.tilbakekreving.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

import no.nav.k9.sak.typer.Periode;

public class BehandlingStatusOgFeilutbetalinger {

    private LocalDate avsluttetDato;

    private List<Periode> feilutbetaltePerioder;

    @JsonCreator
    public BehandlingStatusOgFeilutbetalinger(LocalDate avsluttetDato, List<Periode> feilutbetaltePerioder) {
        this.avsluttetDato = avsluttetDato;
        this.feilutbetaltePerioder = feilutbetaltePerioder;
    }

    public LocalDate getAvsluttetDato() {
        return avsluttetDato;
    }

    public List<Periode> getFeilutbetaltePerioder() {
        return feilutbetaltePerioder;
    }
}
