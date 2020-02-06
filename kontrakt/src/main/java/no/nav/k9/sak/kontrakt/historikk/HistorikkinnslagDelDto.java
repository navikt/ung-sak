package no.nav.k9.sak.kontrakt.historikk;

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

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class HistorikkinnslagDelDto {

    @JsonProperty(value = "begrunnelse")
    @Valid
    private Kodeverdi begrunnelse;

    @JsonProperty(value = "begrunnelseFritekst")
    @Size(max = 5000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelseFritekst;

    @Valid
    @NotNull
    @JsonProperty(value = "hendelse")
    private HistorikkinnslagHendelseDto hendelse;

    @JsonProperty(value = "opplysninger")
    @Valid
    private List<HistorikkinnslagOpplysningDto> opplysninger;

    @JsonProperty(value = "soeknadsperiode")
    @Valid
    private HistorikkinnslagSoeknadsperiodeDto soeknadsperiode;

    @JsonProperty(value = "skjermlenke")
    @Valid
    private SkjermlenkeType skjermlenke;

    @JsonProperty(value = "aarsak")
    @Valid
    private Kodeverdi aarsak;

    @JsonProperty(value = "tema")
    @Valid
    private HistorikkInnslagTemaDto tema;

    @JsonProperty(value = "gjeldendeFra")
    @Valid
    private HistorikkInnslagGjeldendeFraDto gjeldendeFra;

    @JsonProperty(value = "resultat")
    @Size(max = 5000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String resultat;

    @JsonProperty(value = "endredeFelter")
    @Valid
    private List<HistorikkinnslagEndretFeltDto> endredeFelter;

    @JsonProperty(value = "aksjonspunkter")
    @Valid
    private List<HistorikkinnslagTotrinnsVurderingDto> aksjonspunkter;

    public Kodeverdi getBegrunnelse() {
        return begrunnelse;
    }

    public void setBegrunnelse(Kodeverdi begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public void setBegrunnelseFritekst(String begrunnelseFritekst) {
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public HistorikkinnslagHendelseDto getHendelse() {
        return hendelse;
    }

    public void setHendelse(HistorikkinnslagHendelseDto hendelse) {
        this.hendelse = hendelse;
    }

    public SkjermlenkeType getSkjermlenke() {
        return skjermlenke;
    }

    public void setSkjermlenke(SkjermlenkeType skjermlenke) {
        this.skjermlenke = skjermlenke;
    }

    public Kodeverdi getAarsak() {
        return aarsak;
    }

    public void setAarsak(Kodeverdi aarsak) {
        this.aarsak = aarsak;
    }

    public HistorikkInnslagTemaDto getTema() {
        return tema;
    }

    public void setTema(HistorikkInnslagTemaDto tema) {
        this.tema = tema;
    }

    public HistorikkInnslagGjeldendeFraDto getGjeldendeFra() {
        return gjeldendeFra;
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

    public String getResultat() {
        return resultat;
    }

    public void setResultat(String resultat) {
        this.resultat = resultat;
    }

    public List<HistorikkinnslagEndretFeltDto> getEndredeFelter() {
        return endredeFelter;
    }

    public void setEndredeFelter(List<HistorikkinnslagEndretFeltDto> endredeFelter) {
        this.endredeFelter = endredeFelter;
    }

    public List<HistorikkinnslagOpplysningDto> getOpplysninger() {
        return opplysninger;
    }

    public void setOpplysninger(List<HistorikkinnslagOpplysningDto> opplysninger) {
        this.opplysninger = opplysninger;
    }

    public HistorikkinnslagSoeknadsperiodeDto getSoeknadsperiode() {
        return soeknadsperiode;
    }

    public void setSoeknadsperiode(HistorikkinnslagSoeknadsperiodeDto soeknadsperiode) {
        this.soeknadsperiode = soeknadsperiode;
    }

    public List<HistorikkinnslagTotrinnsVurderingDto> getAksjonspunkter() {
        return aksjonspunkter;
    }

    public void setAksjonspunkter(List<HistorikkinnslagTotrinnsVurderingDto> aksjonspunkter) {
        this.aksjonspunkter = aksjonspunkter;
    }

}
