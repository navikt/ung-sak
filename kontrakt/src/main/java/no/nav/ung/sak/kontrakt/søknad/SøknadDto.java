package no.nav.ung.sak.kontrakt.søknad;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.geografisk.Språkkode;
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

    @JsonProperty(value = "manglendeVedlegg")
    @Valid
    @Size(max = 20)
    private List<ManglendeVedleggDto> manglendeVedlegg = new ArrayList<>();

    @JsonProperty(value = "angittePersoner")
    @Valid
    @Size(max = 30)
    private List<AngittPersonDto> angittePersoner = new ArrayList<>();

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

    @JsonProperty(value = "oppgittTilknytning")
    @Valid
    private OppgittTilknytningDto oppgittTilknytning;

    /**
     * Dato søknad sendt fra bruker. (er forskjellig fra mottatdato dersom ikke digital søknad).
     */
    @JsonProperty(value = "soknadsdato", required = true)
    @NotNull
    private LocalDate soknadsdato;

    @JsonProperty(value = "spraakkode")
    @Valid
    private Språkkode spraakkode;

    @JsonProperty(value = "tilleggsopplysninger")
    @Size(max = 5000)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String tilleggsopplysninger;

    @JsonProperty(value = "søknadsperiode")
    @Valid
    private Periode søknadsperiode;

    public SøknadDto() {
    }

    public String getBegrunnelseForSenInnsending() {
        return begrunnelseForSenInnsending;
    }

    public void setBegrunnelseForSenInnsending(String begrunnelseForSenInnsending) {
        this.begrunnelseForSenInnsending = begrunnelseForSenInnsending;
    }

    public List<ManglendeVedleggDto> getManglendeVedlegg() {
        return Collections.unmodifiableList(manglendeVedlegg);
    }

    public void setManglendeVedlegg(List<ManglendeVedleggDto> manglendeVedlegg) {
        this.manglendeVedlegg = List.copyOf(manglendeVedlegg);
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

    public OppgittTilknytningDto getOppgittTilknytning() {
        return oppgittTilknytning;
    }

    public void setOppgittTilknytning(OppgittTilknytningDto oppgittTilknytning) {
        this.oppgittTilknytning = oppgittTilknytning;
    }

    public LocalDate getSoknadsdato() {
        return soknadsdato;
    }

    public void setSoknadsdato(LocalDate soknadsdato) {
        this.soknadsdato = soknadsdato;
    }

    public Språkkode getSpraakkode() {
        return spraakkode;
    }

    public void setSpraakkode(Språkkode spraakkode) {
        this.spraakkode = spraakkode;
    }

    public String getTilleggsopplysninger() {
        return tilleggsopplysninger;
    }

    public void setTilleggsopplysninger(String tilleggsopplysninger) {
        this.tilleggsopplysninger = tilleggsopplysninger;
    }

    public List<AngittPersonDto> getAngittePersoner() {
        return angittePersoner;
    }

    public void setAngittePersoner(Collection<AngittPersonDto> angittePersoner) {
        this.angittePersoner = angittePersoner == null ? Collections.emptyList() : List.copyOf(angittePersoner);
    }

    public void setSøknadsperiode(Periode periode) {
        this.søknadsperiode = periode;
    }
}
