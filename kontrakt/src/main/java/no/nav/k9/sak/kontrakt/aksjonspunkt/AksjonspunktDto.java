package no.nav.k9.sak.kontrakt.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.Objects;
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

    @JsonProperty(value = "aksjonspunktType")
    @Valid
    private AksjonspunktType aksjonspunktType;

    @JsonProperty(value = "begrunnelse")
    @Size(max = 5000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String begrunnelse;

    @JsonProperty(value = "besluttersBegrunnelse")
    @Size(max = 5000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String besluttersBegrunnelse;

    @JsonProperty(value = "definisjon")
    @Valid
    private AksjonspunktDefinisjon definisjon;

    @JsonProperty(value = "erAktivt")
    private Boolean erAktivt;

    @JsonProperty(value = "fristTid")
    private LocalDateTime fristTid;

    @JsonProperty(value = "kanLoses")
    private Boolean kanLoses;

    @JsonProperty(value = "status")
    @Valid
    private AksjonspunktStatus status;

    @JsonProperty(value = "toTrinnsBehandling")
    private Boolean toTrinnsBehandling;

    @JsonProperty(value = "toTrinnsBehandlingGodkjent")
    private Boolean toTrinnsBehandlingGodkjent;

    @JsonAlias("vilkårType")
    @JsonProperty(value = "vilkarType")
    private VilkårType vilkarType;

    @JsonProperty(value = "vurderPaNyttArsaker")
    @Valid
    @Size(max = 100)
    private Set<VurderÅrsak> vurderPaNyttArsaker;

    public AksjonspunktDto() {
    }

    public AksjonspunktType getAksjonspunktType() {
        return aksjonspunktType;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getBesluttersBegrunnelse() {
        return besluttersBegrunnelse;
    }

    public AksjonspunktDefinisjon getDefinisjon() {
        return definisjon;
    }

    public Boolean getErAktivt() {
        return erAktivt;
    }

    public LocalDateTime getFristTid() {
        return fristTid;
    }

    public Boolean getKanLoses() {
        return kanLoses;
    }

    public AksjonspunktStatus getStatus() {
        return status;
    }

    public Boolean getToTrinnsBehandling() {
        return toTrinnsBehandling;
    }

    public Boolean getToTrinnsBehandlingGodkjent() {
        return toTrinnsBehandlingGodkjent;
    }

    public VilkårType getVilkarType() {
        return vilkarType;
    }

    public Set<VurderÅrsak> getVurderPaNyttArsaker() {
        return vurderPaNyttArsaker;
    }

    public void setAksjonspunktType(AksjonspunktType aksjonspunktType) {
        this.aksjonspunktType = aksjonspunktType;
    }

    public void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public void setBesluttersBegrunnelse(String besluttersBegrunnelse) {
        this.besluttersBegrunnelse = besluttersBegrunnelse;
    }

    public void setDefinisjon(AksjonspunktDefinisjon definisjon) {
        this.definisjon = definisjon;
    }

    public void setErAktivt(Boolean erAktivt) {
        this.erAktivt = erAktivt;
    }

    public void setFristTid(LocalDateTime fristTid) {
        this.fristTid = fristTid;
    }

    public void setKanLoses(Boolean kanLoses) {
        this.kanLoses = kanLoses;
    }

    public void setStatus(AksjonspunktStatus status) {
        this.status = status;
    }

    public void setToTrinnsBehandling(Boolean toTrinnsBehandling) {
        this.toTrinnsBehandling = toTrinnsBehandling;
    }

    public void setToTrinnsBehandlingGodkjent(Boolean toTrinnsBehandlingGodkjent) {
        this.toTrinnsBehandlingGodkjent = toTrinnsBehandlingGodkjent;
    }

    public void setVilkarType(VilkårType vilkarType) {
        this.vilkarType = vilkarType;
    }

    public void setVurderPaNyttArsaker(Set<VurderÅrsak> vurderPaNyttArsaker) {
        this.vurderPaNyttArsaker = vurderPaNyttArsaker;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var other = (AksjonspunktDto) obj;

        return Objects.equals(aksjonspunktType, other.aksjonspunktType)
            && Objects.equals(definisjon, other.definisjon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aksjonspunktType, definisjon);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
            + "def=" + definisjon
            + ", type=" + aksjonspunktType
            + ", erAktivt=" + erAktivt
            + ", fristTid=" + fristTid
            + ", status=" + status
            + ", kanLoses=" + kanLoses
            + ", toTrinnsBehandling=" + toTrinnsBehandling
            + ", toTrinnsBehandlingGodkjent=" + toTrinnsBehandlingGodkjent
            + ", vilkarType=" + vilkarType
            + ", begrunnelse=" + begrunnelse
            + ", besluttersBegrunnelse=" + besluttersBegrunnelse
            + ", vurderPaNyttArsaker=" + vurderPaNyttArsaker
            + ">";
    }
}
