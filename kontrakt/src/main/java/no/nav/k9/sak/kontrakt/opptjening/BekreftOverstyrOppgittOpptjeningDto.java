package no.nav.k9.sak.kontrakt.opptjening;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
@JsonTypeName(AksjonspunktKodeDefinisjon.OVERSTYRING_FRISINN_OPPGITT_OPPTJENING_KODE)
public class BekreftOverstyrOppgittOpptjeningDto extends BekreftetAksjonspunktDto {

    public BekreftOverstyrOppgittOpptjeningDto() {
        //jackson
    }

    public BekreftOverstyrOppgittOpptjeningDto(String begrunnelse, @JsonProperty(value = "oppgittOpptjening", required = true) @NotNull @Valid OppgittOpptjeningDto oppgittOpptjening) {
        super(begrunnelse);
        this.oppgittOpptjening = oppgittOpptjening;
    }

    @JsonProperty(value = "oppgittOpptjening", required = true)
    @NotNull
    @Valid
    private OppgittOpptjeningDto oppgittOpptjening;

    public OppgittOpptjeningDto getOppgittOpptjening() {
        return oppgittOpptjening;
    }

    public void setOppgittOpptjening(OppgittOpptjeningDto oppgittOpptjening) {
        this.oppgittOpptjening = oppgittOpptjening;
    }
}
