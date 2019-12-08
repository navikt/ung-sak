package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class SoknadDto {

    /** Dato søknad mottatt av Nav. */
    @JsonProperty(value = "mottattDato")
    private LocalDate mottattDato;
    
    /** Dato søknad sendt fra bruker. (er forskjellig fra mottatdato dersom ikke digital søknad). */
    @JsonProperty(value = "soknadsdato")
    private LocalDate soknadsdato;
    
    /** Oppgitt startdato for ytelsen fra søknad. */
    @JsonProperty(value = "oppgittStartdato")
    private LocalDate oppgittStartdato;
    
    @JsonProperty(value = "tilleggsopplysninger")
    private String tilleggsopplysninger;
    
    @JsonProperty(value = "begrunnelseForSenInnsending")
    private String begrunnelseForSenInnsending;

    @JsonProperty(value = "oppgittTilknytning")
    private OppgittTilknytningDto oppgittTilknytning;
    
    @JsonProperty(value = "manglendeVedlegg")
    private List<ManglendeVedleggDto> manglendeVedlegg;
    
    @JsonProperty(value = "spraakkode")
    private Språkkode spraakkode;

    protected SoknadDto() {
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
