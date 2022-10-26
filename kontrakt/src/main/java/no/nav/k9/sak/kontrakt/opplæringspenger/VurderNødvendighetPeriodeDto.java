package no.nav.k9.sak.kontrakt.opplæringspenger;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.kontrakt.dokument.TekstValideringRegex;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class VurderNødvendighetPeriodeDto {

    @JsonProperty(value = "nødvendigOpplæring")
    private boolean nødvendigOpplæring;

    @JsonProperty(value = "fom")
    private LocalDate fom;

    @JsonProperty(value = "tom")
    private LocalDate tom;

    @JsonProperty("begrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = TekstValideringRegex.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelse;

    @JsonProperty(value = "institusjon")
    @Size(max = 100)
    @Pattern(regexp = TekstValideringRegex.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String institusjon;

    public VurderNødvendighetPeriodeDto() {
    }

    public VurderNødvendighetPeriodeDto(boolean nødvendigOpplæring, LocalDate fom, LocalDate tom, String begrunnelse, String institusjon) {
        this.nødvendigOpplæring = nødvendigOpplæring;
        this.fom = fom;
        this.tom = tom;
        this.begrunnelse = begrunnelse;
        this.institusjon = institusjon;
    }

    public boolean isNødvendigOpplæring() {
        return nødvendigOpplæring;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getInstitusjon() {
        return institusjon;
    }
}
