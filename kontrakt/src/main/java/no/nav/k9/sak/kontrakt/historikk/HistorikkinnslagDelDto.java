package no.nav.k9.sak.kontrakt.historikk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.sak.kontrakt.Patterns;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class HistorikkinnslagDelDto {

    @JsonProperty(value = "aarsak")
    @Valid
    private Kodeverdi aarsak;

    @JsonProperty(value = "aksjonspunkter")
    @Valid
    private List<HistorikkinnslagTotrinnsVurderingDto> aksjonspunkter;

    @JsonProperty(value = "begrunnelse")
    @Valid
    private Kodeverdi begrunnelse;

    @JsonProperty(value = "begrunnelseFritekst")
    @Size(max = 4000)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String begrunnelseFritekst;

    @JsonProperty(value = "endredeFelter")
    @Valid
    private List<HistorikkinnslagEndretFeltDto> endredeFelter = new ArrayList<>();

    @JsonProperty(value = "gjeldendeFra")
    @Valid
    private HistorikkInnslagGjeldendeFraDto gjeldendeFra;

    @Valid
    @NotNull
    @JsonProperty(value = "hendelse")
    private HistorikkinnslagHendelseDto hendelse;

    @JsonProperty(value = "opplysninger")
    @Valid
    private List<HistorikkinnslagOpplysningDto> opplysninger = new ArrayList<>();

    @JsonProperty(value = "resultat")
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String resultat;

    @JsonProperty(value = "skjermlenke")
    @Valid
    private SkjermlenkeType skjermlenke;

    @JsonProperty(value = "soeknadsperiode")
    @Valid
    private HistorikkinnslagSoeknadsperiodeDto soeknadsperiode;

    @JsonProperty(value = "tema")
    @Valid
    private HistorikkInnslagTemaDto tema;

    public HistorikkinnslagDelDto() {
        //
    }

    public Kodeverdi getAarsak() {
        return aarsak;
    }

    public List<HistorikkinnslagTotrinnsVurderingDto> getAksjonspunkter() {
        return Collections.unmodifiableList(aksjonspunkter);
    }

    public Kodeverdi getBegrunnelse() {
        return begrunnelse;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public List<HistorikkinnslagEndretFeltDto> getEndredeFelter() {
        return Collections.unmodifiableList(endredeFelter);
    }

    public HistorikkInnslagGjeldendeFraDto getGjeldendeFra() {
        return gjeldendeFra;
    }

    public HistorikkinnslagHendelseDto getHendelse() {
        return hendelse;
    }

    public List<HistorikkinnslagOpplysningDto> getOpplysninger() {
        return Collections.unmodifiableList(opplysninger);
    }

    public String getResultat() {
        return resultat;
    }

    public SkjermlenkeType getSkjermlenke() {
        return skjermlenke;
    }

    public HistorikkinnslagSoeknadsperiodeDto getSoeknadsperiode() {
        return soeknadsperiode;
    }

    public HistorikkInnslagTemaDto getTema() {
        return tema;
    }

    public void setAarsak(Kodeverdi aarsak) {
        this.aarsak = aarsak;
    }

    public void setAksjonspunkter(List<HistorikkinnslagTotrinnsVurderingDto> aksjonspunkter) {
        this.aksjonspunkter = List.copyOf(aksjonspunkter);
    }

    public void setBegrunnelse(Kodeverdi begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setBegrunnelseFritekst(String begrunnelseFritekst) {
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public void setEndredeFelter(List<HistorikkinnslagEndretFeltDto> endredeFelter) {
        this.endredeFelter = List.copyOf(endredeFelter);
    }

    public void setGjeldendeFra(HistorikkInnslagGjeldendeFraDto gjeldendeFra) {
        this.gjeldendeFra = gjeldendeFra;
    }

    public void setGjeldendeFra(String fra) {
        if (this.gjeldendeFra == null) {
            this.gjeldendeFra = new HistorikkInnslagGjeldendeFraDto(fra);
        } else {
            this.gjeldendeFra.setFra(fra);
        }
    }

    public void setGjeldendeFra(String fra, String navn, String verdi) {
        if (this.gjeldendeFra == null) {
            this.gjeldendeFra = new HistorikkInnslagGjeldendeFraDto(fra, navn, verdi);
        } else {
            this.gjeldendeFra.setFra(fra);
            this.gjeldendeFra.setNavn(navn);
            this.gjeldendeFra.setVerdi(verdi);
        }
    }

    public void setHendelse(HistorikkinnslagHendelseDto hendelse) {
        this.hendelse = hendelse;
    }

    public void setOpplysninger(List<HistorikkinnslagOpplysningDto> opplysninger) {
        this.opplysninger = List.copyOf(opplysninger);
    }

    public void setResultat(String resultat) {
        this.resultat = resultat;
    }

    public void setSkjermlenke(SkjermlenkeType skjermlenke) {
        this.skjermlenke = skjermlenke;
    }

    public void setSoeknadsperiode(HistorikkinnslagSoeknadsperiodeDto soeknadsperiode) {
        this.soeknadsperiode = soeknadsperiode;
    }

    public void setTema(HistorikkInnslagTemaDto tema) {
        this.tema = tema;
    }

}
