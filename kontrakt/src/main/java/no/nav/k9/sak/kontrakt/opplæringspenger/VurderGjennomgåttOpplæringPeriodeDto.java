package no.nav.k9.sak.kontrakt.opplæringspenger;

import java.time.LocalDate;

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
public class VurderGjennomgåttOpplæringPeriodeDto {

    @JsonProperty(value = "periode", required = true)
    @NotNull
    @Valid
    private Periode periode;

    @JsonProperty(value = "gjennomførtOpplæring", required = true)
    @NotNull
    private Boolean gjennomførtOpplæring;

    @JsonProperty(value = "reisetid", required = true)
    @Valid
    private VurderReisetidDto reisetid;

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = TekstValideringRegex.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    public VurderGjennomgåttOpplæringPeriodeDto() {
    }

    public VurderGjennomgåttOpplæringPeriodeDto(Periode periode, Boolean gjennomførtOpplæring, VurderReisetidDto reisetid, String begrunnelse) {
        this.periode = periode;
        this.gjennomførtOpplæring = gjennomførtOpplæring;
        this.reisetid = reisetid;
        this.begrunnelse = begrunnelse;
    }

    public VurderGjennomgåttOpplæringPeriodeDto(LocalDate fom, LocalDate tom, Boolean gjennomførtOpplæring, VurderReisetidDto reisetid, String begrunnelse) {
        this.periode = new Periode(fom, tom);
        this.gjennomførtOpplæring = gjennomførtOpplæring;
        this.reisetid = reisetid;
        this.begrunnelse = begrunnelse;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Boolean getGjennomførtOpplæring() {
        return gjennomførtOpplæring;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public VurderReisetidDto getReisetid() {
        return reisetid;
    }
}
