package no.nav.k9.sak.kontrakt.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.k9.kodeverk.vilkår.VilkårType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AksjonspunktDto {

    @JsonProperty(value = "definisjon")
    @Valid
    private AksjonspunktDefinisjon definisjon;

    @JsonProperty(value = "status")
    @Valid
    private AksjonspunktStatus status;
    
    @JsonProperty(value = "begrunnelse")
    @Size(max=5000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message="'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;
    
    @JsonAlias("vilkårType")
    @JsonProperty(value = "vilkarType")
    private VilkårType vilkarType;

    @JsonProperty(value="toTrinnsBehandling")
    private Boolean toTrinnsBehandling;
    
    @JsonProperty(value="toTrinnsBehandlingGodkjent")
    private Boolean toTrinnsBehandlingGodkjent;
    
    @JsonProperty(value="vurderPaNyttArsaker")
    @Valid
    @Size(max=100)
    private Set<VurderÅrsak> vurderPaNyttArsaker;
    
    @JsonProperty(value="besluttersBegrunnelse")
    @Size(max=5000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message="'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String besluttersBegrunnelse;
    
    @JsonProperty(value="aksjonspunktType")
    @Valid
    private AksjonspunktType aksjonspunktType;
    
    @JsonProperty(value="kanLoses")
    private Boolean kanLoses;
    
    @JsonProperty(value="erAktivt")
    private Boolean erAktivt;
    
    @JsonProperty(value="fristTid")
    private LocalDateTime fristTid;

    public AksjonspunktDto() {
    }

    public void setDefinisjon(AksjonspunktDefinisjon definisjon) {
        this.definisjon = definisjon;
    }

    public void setStatus(AksjonspunktStatus status) {
        this.status = status;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setVilkarType(VilkårType vilkarType) {
        this.vilkarType = vilkarType;
    }

    public void setToTrinnsBehandling(Boolean toTrinnsBehandling) {
        this.toTrinnsBehandling = toTrinnsBehandling;
    }

    public void setToTrinnsBehandlingGodkjent(Boolean toTrinnsBehandlingGodkjent) {
        this.toTrinnsBehandlingGodkjent = toTrinnsBehandlingGodkjent;
    }

    public void setVurderPaNyttArsaker(Set<VurderÅrsak> vurderPaNyttArsaker) {
        this.vurderPaNyttArsaker = vurderPaNyttArsaker;
    }

    public void setBesluttersBegrunnelse(String besluttersBegrunnelse) {
        this.besluttersBegrunnelse = besluttersBegrunnelse;
    }

    public void setAksjonspunktType(AksjonspunktType aksjonspunktType) {
        this.aksjonspunktType = aksjonspunktType;
    }

    public void setKanLoses(Boolean kanLoses) {
        this.kanLoses = kanLoses;
    }

    public void setErAktivt(Boolean erAktivt) {
        this.erAktivt = erAktivt;
    }

    public AksjonspunktDefinisjon getDefinisjon() {
        return definisjon;
    }

    public VilkårType getVilkarType() {
        return vilkarType;
    }

    public AksjonspunktStatus getStatus() {
        return status;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public Boolean getToTrinnsBehandling() {
        return toTrinnsBehandling;
    }

    public Set<VurderÅrsak> getVurderPaNyttArsaker() {
        return vurderPaNyttArsaker;
    }

    public String getBesluttersBegrunnelse() {
        return besluttersBegrunnelse;
    }

    public Boolean getToTrinnsBehandlingGodkjent() {
        return toTrinnsBehandlingGodkjent;
    }

    public AksjonspunktType getAksjonspunktType() {
        return aksjonspunktType;
    }

    public Boolean getKanLoses() {
        return kanLoses;
    }

    public Boolean getErAktivt() {
        return erAktivt;
    }

    public LocalDateTime getFristTid() {
        return fristTid;
    }

    public void setFristTid(LocalDateTime fristTid) {
        this.fristTid = fristTid;
    }
}
