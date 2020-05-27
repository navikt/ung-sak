package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE)
public class VurderVarigEndringEllerNyoppstartetSNDtoer extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "grunnlag")
    @Valid
    @NotNull
    @Size(min = 1)
    private List<VurderVarigEndringEllerNyoppstartetSNDto> grunnlag;

    public VurderVarigEndringEllerNyoppstartetSNDtoer() {
        //
    }

    public VurderVarigEndringEllerNyoppstartetSNDtoer(String begrunnelse, List<VurderVarigEndringEllerNyoppstartetSNDto> grunnlag) {
        super(begrunnelse);
        this.grunnlag = grunnlag;
    }

    public List<VurderVarigEndringEllerNyoppstartetSNDto> getGrunnlag() {
        return grunnlag;
    }
}
