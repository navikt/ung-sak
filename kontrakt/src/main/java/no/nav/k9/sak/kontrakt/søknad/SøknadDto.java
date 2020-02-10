package no.nav.k9.sak.kontrakt.søknad;

import java.time.LocalDate;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.geografisk.Språkkode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape=Shape.OBJECT)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class SøknadDto {

    /** Dato søknad mottatt av Nav. */
    @JsonProperty(value = "mottattDato", required = true)
    @NotNull
    private LocalDate mottattDato;

    /** Dato søknad sendt fra bruker. (er forskjellig fra mottatdato dersom ikke digital søknad). */
    @JsonProperty(value = "soknadsdato", required = true)
    @NotNull
    private LocalDate soknadsdato;

    /** Oppgitt startdato for ytelsen fra søknad. */
    @JsonProperty(value = "oppgittStartdato", required = true)
    @NotNull
    private LocalDate oppgittStartdato;

    @JsonProperty(value = "tilleggsopplysninger")
    @Size(max = 5000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String tilleggsopplysninger;

    @JsonProperty(value = "begrunnelseForSenInnsending")
    @Size(max = 5000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelseForSenInnsending;

    @JsonProperty(value = "oppgittTilknytning")
    @Valid
    private OppgittTilknytningDto oppgittTilknytning;

    @JsonProperty(value = "manglendeVedlegg")
    @Valid
    @Size(max = 20)
    private List<ManglendeVedleggDto> manglendeVedlegg;

    @JsonProperty(value = "spraakkode")
    @Valid
    private Språkkode spraakkode;

    public SøknadDto() {
    }

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public LocalDate getSoknadsdato() {
        return soknadsdato;
    }

    public String getTilleggsopplysninger() {
        return tilleggsopplysninger;
    }

    public String getBegrunnelseForSenInnsending() {
        return begrunnelseForSenInnsending;
    }

    public OppgittTilknytningDto getOppgittTilknytning() {
        return oppgittTilknytning;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public void setTilleggsopplysninger(String tilleggsopplysninger) {
        this.tilleggsopplysninger = tilleggsopplysninger;
    }

    public void setBegrunnelseForSenInnsending(String begrunnelseForSenInnsending) {
        this.begrunnelseForSenInnsending = begrunnelseForSenInnsending;
    }

    public void setOppgittTilknytning(OppgittTilknytningDto oppgittTilknytning) {
        this.oppgittTilknytning = oppgittTilknytning;
    }

    public List<ManglendeVedleggDto> getManglendeVedlegg() {
        return manglendeVedlegg;
    }

    public void setOppgittStartdato(LocalDate oppgittStartdato) {
        this.oppgittStartdato = oppgittStartdato;
    }

    public LocalDate getOppgittStartdato() {
        return oppgittStartdato;
    }

    public void setManglendeVedlegg(List<ManglendeVedleggDto> manglendeVedlegg) {
        this.manglendeVedlegg = manglendeVedlegg;
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
}
