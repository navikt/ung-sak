package no.nav.k9.sak.kontrakt.omsorg;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OmsorgenForOversiktDto {

    @JsonProperty(value = "registrertForeldrerelasjon")
    @Valid
    private boolean registrertForeldrerelasjon;
    
    @JsonProperty(value = "registrertSammeBosted")
    @Valid
    private boolean registrertSammeBosted;
    
    @JsonProperty(value = "tvingManuellVurdering")
    @Valid
    private boolean tvingManuellVurdering;
    
    @JsonProperty(value = "kanLøseAksjonspunkt")
    @Valid
    private boolean kanLøseAksjonspunkt;
    
    @JsonProperty(value = "omsorgsperioder")
    @Size(max = 1000)
    @Valid
    private List<OmsorgenForDto> omsorgsperioder = new ArrayList<>();

    
    public OmsorgenForOversiktDto(boolean registrertForeldrerelasjon, boolean registrertSammeBosted, boolean tvingManuellVurdering, boolean kanLøseAksjonspunkt, List<OmsorgenForDto> omsorgsperioder) {
        this.registrertForeldrerelasjon = registrertForeldrerelasjon;
        this.registrertSammeBosted = registrertSammeBosted;
        this.tvingManuellVurdering = tvingManuellVurdering;
        this.kanLøseAksjonspunkt = kanLøseAksjonspunkt;
        this.omsorgsperioder = omsorgsperioder;
    }

    OmsorgenForOversiktDto() {
        
    }
    
    
    public boolean isRegistrertForeldrerelasjon() {
        return registrertForeldrerelasjon;
    }
    
    public boolean isSammeBosted() {
        return registrertSammeBosted;
    }
    
    public boolean isTvingManuellVurdering() {
        return tvingManuellVurdering;
    }
    
    public boolean isKanLøseAksjonspunkt() {
        return kanLøseAksjonspunkt;
    }
    
    public List<OmsorgenForDto> getOmsorgsperioder() {
        return omsorgsperioder;
    }
}
