package no.nav.k9.sak.kontrakt.omsorg;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.k9.sak.kontrakt.person.NorskIdentDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.AVKLAR_OMSORGEN_FOR_KODE_V2)
public class AvklarOmsorgenForDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "omsorgsperioder")
    @Size(max = 1000)
    @Valid
    private List<OmsorgenForOppdateringDto> omsorgsperioder = new ArrayList<>();

    @JsonProperty(value = "fosterbarnForOmsorgspenger", required = true)
    @Valid
    @Size(max = 100)
    private List<NorskIdentDto> fosterbarnForOmsorgspenger;

    public AvklarOmsorgenForDto(String begrunnelse, List<OmsorgenForOppdateringDto> omsorgsperioder,
                                List<NorskIdentDto> fosterbarnForOmsorgspenger) {
        super(begrunnelse);
        this.omsorgsperioder = omsorgsperioder;
        this.fosterbarnForOmsorgspenger = fosterbarnForOmsorgspenger;
    }

    protected AvklarOmsorgenForDto() {
        //
    }

    public List<OmsorgenForOppdateringDto> getOmsorgsperioder() {
        return omsorgsperioder;
    }

    public List<NorskIdentDto> getFosterbarnForOmsorgspenger() {
        return fosterbarnForOmsorgspenger;
    }
}
