package no.nav.ung.sak.kontrakt.vedtak;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.ung.sak.kontrakt.Patterns;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class TotrinnskontrollAksjonspunkterDto {

    public static class Builder {
        TotrinnskontrollAksjonspunkterDto kladd = new TotrinnskontrollAksjonspunkterDto();

        public TotrinnskontrollAksjonspunkterDto build() {
            return kladd;
        }

        public Builder medAksjonspunktKode(AksjonspunktDefinisjon aksjonspunktKode) {
            kladd.aksjonspunktKode = aksjonspunktKode.getKode();
            return this;
        }

        public Builder medBesluttersBegrunnelse(String besluttersBegrunnelse) {
            kladd.besluttersBegrunnelse = besluttersBegrunnelse;
            return this;
        }

        public Builder medTotrinnskontrollGodkjent(Boolean totrinnskontrollGodkjent) {
            kladd.totrinnskontrollGodkjent = totrinnskontrollGodkjent;
            return this;
        }

        public Builder medVurderPaNyttArsaker(Set<VurderÅrsak> vurderPaNyttArsaker) {
            kladd.vurderPaNyttArsaker = vurderPaNyttArsaker;
            return this;
        }
    }

    @JsonProperty(value = "aksjonspunktKode")
    @Size(max = 10)
    @Pattern(regexp = "^[\\p{Alnum}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String aksjonspunktKode;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "beregningDto")
    @Valid
    private TotrinnsBeregningDto beregningDto;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "beregningDtoer")
    @Valid
    private List<TotrinnsBeregningDto> beregningDtoer;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "besluttersBegrunnelse")
    @Size(max = 4000)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String besluttersBegrunnelse;

    @JsonProperty(value = "totrinnskontrollGodkjent")
    private Boolean totrinnskontrollGodkjent;

    @JsonProperty(value = "vurderPaNyttArsaker")
    @Size(max = 100)
    @Valid
    private Set<VurderÅrsak> vurderPaNyttArsaker = new HashSet<>();

    public TotrinnskontrollAksjonspunkterDto() {
        //
    }

    public AksjonspunktDefinisjon getAksjonspunktKode() {
        return AksjonspunktDefinisjon.fraKode(aksjonspunktKode);
    }

    public TotrinnsBeregningDto getBeregningDto() {
        return beregningDto;
    }

    public String getBesluttersBegrunnelse() {
        return besluttersBegrunnelse;
    }

    public List<TotrinnsBeregningDto> getBeregningDtoer() {
        return beregningDtoer;
    }

    public Boolean getTotrinnskontrollGodkjent() {
        return totrinnskontrollGodkjent;
    }

    public Set<VurderÅrsak> getVurderPaNyttArsaker() {
        return Collections.unmodifiableSet(vurderPaNyttArsaker);
    }

    public void setAksjonspunktKode(String aksjonspunktKode) {
        this.aksjonspunktKode = aksjonspunktKode;
    }

    public void setBeregningDto(TotrinnsBeregningDto beregningDto) {
        this.beregningDto = beregningDto;
    }

    public void setBesluttersBegrunnelse(String besluttersBegrunnelse) {
        this.besluttersBegrunnelse = besluttersBegrunnelse;
    }

    public void setTotrinnskontrollGodkjent(Boolean totrinnskontrollGodkjent) {
        this.totrinnskontrollGodkjent = totrinnskontrollGodkjent;
    }

    public void setVurderPaNyttArsaker(Set<VurderÅrsak> vurderPaNyttArsaker) {
        this.vurderPaNyttArsaker = vurderPaNyttArsaker;
    }
}
