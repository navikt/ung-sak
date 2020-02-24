package no.nav.k9.sak.kontrakt.opptjening;

import java.util.List;

import javax.validation.Valid;
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
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_PERIODER_MED_OPPTJENING_KODE)
public class AvklarAktivitetsPerioderDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "opptjeningAktivitetList")
    @Valid
    @Size(max = 100)
    private List<AvklarOpptjeningAktivitetDto> opptjeningAktivitetList;

    public AvklarAktivitetsPerioderDto() {
        // For Jackson
    }

    public AvklarAktivitetsPerioderDto(String begrunnelse, List<AvklarOpptjeningAktivitetDto> opptjeningAktivitetList) {
        super(begrunnelse);
        this.opptjeningAktivitetList = opptjeningAktivitetList;
    }

    public List<AvklarOpptjeningAktivitetDto> getOpptjeningAktivitetList() {
        return opptjeningAktivitetList;
    }

    public void setOpptjeningAktivitetList(List<AvklarOpptjeningAktivitetDto> opptjeningAktivitetList) {
        this.opptjeningAktivitetList = opptjeningAktivitetList;
    }

}
