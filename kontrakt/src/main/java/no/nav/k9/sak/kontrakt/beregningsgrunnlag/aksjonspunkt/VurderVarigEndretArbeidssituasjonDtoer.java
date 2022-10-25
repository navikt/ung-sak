package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_VARIG_ENDRET_ARBEIDSSITUASJON_KODE)
public class VurderVarigEndretArbeidssituasjonDtoer extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "grunnlag")
    @Valid
    @NotNull
    @Size(min = 1)
    private List<VurderVarigEndretArbeidssituasjonDto> grunnlag;

    public VurderVarigEndretArbeidssituasjonDtoer() {
        //
    }

    public VurderVarigEndretArbeidssituasjonDtoer(String begrunnelse, List<VurderVarigEndretArbeidssituasjonDto> grunnlag) {
        super(begrunnelse);
        this.grunnlag = grunnlag;
    }

    public List<VurderVarigEndretArbeidssituasjonDto> getGrunnlag() {
        return grunnlag;
    }
}
