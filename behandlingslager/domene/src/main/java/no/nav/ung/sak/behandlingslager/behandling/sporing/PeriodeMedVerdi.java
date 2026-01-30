package no.nav.ung.sak.behandlingslager.behandling.sporing;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.felles.validering.InputValideringRegex;
import no.nav.ung.sak.felles.typer.Periode;

import java.time.LocalDate;

class PeriodeMedVerdi extends Periode {

    @JsonProperty(value = "verdi", required = true)
    @Valid
    //klassen er en subklasse av Periode, som brukes som input-DTO. Derfor vil testen flagge denne som en input-DTO, og kreve validering-annoteringer
    //valideringer brukes bare ved mottak på REST, så det har ingen effekt slik denne klassen brukes i dag
    @Size(min = 0, max = 4000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String verdi;

    public PeriodeMedVerdi(LocalDate fom, LocalDate tom, String verdi) {
        super(fom, tom);
        this.verdi = verdi;
    }

    public String getVerdi() {
        return verdi;
    }


}

