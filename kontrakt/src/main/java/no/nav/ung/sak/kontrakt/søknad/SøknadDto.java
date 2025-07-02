package no.nav.ung.sak.kontrakt.søknad;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.geografisk.Språkkode;
import no.nav.ung.sak.kontrakt.Patterns;
import no.nav.ung.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class SøknadDto {

    @JsonProperty(value = "begrunnelseForSenInnsending")
    @Size(max = 5000)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelseForSenInnsending;

    /**
     * Dato søknad mottatt av Nav.
     */
    @JsonProperty(value = "mottattDato", required = true)
    @NotNull
    private LocalDate mottattDato;

    /**
     * Oppgitt startdato for ytelsen fra søknad.
     */
    @JsonProperty(value = "oppgittStartdato", required = true)
    @NotNull
    private LocalDate oppgittStartdato;

    @JsonProperty(value = "spraakkode")
    @Valid
    private Språkkode spraakkode;

    public SøknadDto() {
    }

    public String getBegrunnelseForSenInnsending() {
        return begrunnelseForSenInnsending;
    }

    public void setBegrunnelseForSenInnsending(String begrunnelseForSenInnsending) {
        this.begrunnelseForSenInnsending = begrunnelseForSenInnsending;
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public LocalDate getOppgittStartdato() {
        return oppgittStartdato;
    }

    public void setOppgittStartdato(LocalDate oppgittStartdato) {
        this.oppgittStartdato = oppgittStartdato;
    }

    public Språkkode getSpraakkode() {
        return spraakkode;
    }

    public void setSpraakkode(Språkkode spraakkode) {
        this.spraakkode = spraakkode;
    }
}
