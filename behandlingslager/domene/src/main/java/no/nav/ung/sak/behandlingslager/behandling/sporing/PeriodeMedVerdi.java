package no.nav.ung.sak.behandlingslager.behandling.sporing;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;

class PeriodeMedVerdi extends Periode {

    @JsonProperty(value = "verdi", required = true)
    @Valid
    private String verdi;

    public PeriodeMedVerdi(LocalDate fom, LocalDate tom, String verdi) {
        super(fom, tom);
        this.verdi = verdi;
    }

    public String getVerdi() {
        return verdi;
    }


}

