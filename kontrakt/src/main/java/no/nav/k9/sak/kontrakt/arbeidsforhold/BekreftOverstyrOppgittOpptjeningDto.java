package no.nav.k9.sak.kontrakt.arbeidsforhold;

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

    public BekreftOverstyrOppgittOpptjeningDto(String begrunnelse, @JsonProperty(value = "søknadsperiodeOgOppgittOpptjeningDto", required = true) @NotNull @Valid SøknadsperiodeOgOppgittOpptjeningDto søknadsperiodeOgOppgittOpptjeningDto) {
        super(begrunnelse);
        this.søknadsperiodeOgOppgittOpptjeningDto = søknadsperiodeOgOppgittOpptjeningDto;
    }

    @JsonProperty(value = "søknadsperiodeOgOppgittOpptjeningDto", required = true)
    @NotNull
    @Valid
    private SøknadsperiodeOgOppgittOpptjeningDto søknadsperiodeOgOppgittOpptjeningDto;

    public SøknadsperiodeOgOppgittOpptjeningDto getSøknadsperiodeOgOppgittOpptjeningDto() {
        return søknadsperiodeOgOppgittOpptjeningDto;
    }

    public void setSøknadsperiodeOgOppgittOpptjeningDto(SøknadsperiodeOgOppgittOpptjeningDto søknadsperiodeOgOppgittOpptjeningDto) {
        this.søknadsperiodeOgOppgittOpptjeningDto = søknadsperiodeOgOppgittOpptjeningDto;
    }
}
