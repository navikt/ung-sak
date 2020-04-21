package no.nav.k9.sak.kontrakt.søknad;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.AvslagbartAksjonspunktDto;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU_KODE)
public class BekreftSokersOpplysningspliktManuDto extends BekreftetAksjonspunktDto implements AvslagbartAksjonspunktDto {

    @JsonProperty(value = "erVilkarOk", required = true)
    @NotNull
    private Boolean erVilkarOk;

    @JsonProperty(value = "inntektsmeldingerSomIkkeKommer")
    @Valid
    @Size(max = 50)
    private List<InntektsmeldingSomIkkeKommerDto> inntektsmeldingerSomIkkeKommer;

    public BekreftSokersOpplysningspliktManuDto() { // NOSONAR
        // For Jackson
    }

    public BekreftSokersOpplysningspliktManuDto(String begrunnelse, Boolean erVilkarOk, List<InntektsmeldingSomIkkeKommerDto> inntektsmeldingerSomIkkeKommer) {
        super(begrunnelse);
        this.erVilkarOk = erVilkarOk;
        this.inntektsmeldingerSomIkkeKommer = inntektsmeldingerSomIkkeKommer;
    }

    @JsonIgnore
    @Override
    public String getAvslagskode() {
        return null; // støttes ikke
    }

    @Override
    public Boolean getErVilkarOk() {
        return erVilkarOk;
    }

    public List<InntektsmeldingSomIkkeKommerDto> getInntektsmeldingerSomIkkeKommer() {
        return Collections.unmodifiableList(inntektsmeldingerSomIkkeKommer);
    }

    public void setErVilkarOk(Boolean erVilkarOk) {
        this.erVilkarOk = erVilkarOk;
    }

    public void setInntektsmeldingerSomIkkeKommer(List<InntektsmeldingSomIkkeKommerDto> inntektsmeldingerSomIkkeKommer) {
        this.inntektsmeldingerSomIkkeKommer = inntektsmeldingerSomIkkeKommer;
    }
    

    @Override
    public boolean equals(Object obj) {
        if(obj==this) return true;
        if(obj ==null || obj.getClass()!=this.getClass()) return false;
        var other = (BekreftSokersOpplysningspliktManuDto) obj;
        return Objects.equals(getKode(), other.getKode());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getKode());
    }
    
    @Override
    public String toString() {
        return getClass() + "<kode=" + getKode() + ", begrunnelse=" + getBegrunnelse() + ">";
    }
}
