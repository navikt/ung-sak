package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET_KODE)
public class FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDtoer extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "grunnlag")
    @Valid
    @NotNull
    @Size(min = 1)
    private List<FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto> grunnlag;

    public FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDtoer() {
        // For Jackson
    }

    public FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDtoer(String begrunnelse, List<FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto> grunnlag) {
        super(begrunnelse);
        this.grunnlag = grunnlag;
    }

    public List<FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto> getGrunnlag() {
        return grunnlag;
    }
}
