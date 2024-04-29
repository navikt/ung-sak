package no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertFalse;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_DATO_NY_REGEL_UTTAK)
public class VurderVirkningsdatoUttakNyeReglerDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "virkningsdato")
    @Valid
    @NotNull
    private LocalDate virkningsdato;

    public VurderVirkningsdatoUttakNyeReglerDto() {
        //
    }

    public VurderVirkningsdatoUttakNyeReglerDto(String begrunnelse, LocalDate virkningsdato) {
        super(begrunnelse);
        this.virkningsdato = virkningsdato;
    }

    public LocalDate getVirkningsdato() {
        return virkningsdato;
    }

    @AssertFalse(message = "Virkningsdato er utenfor gyldig intervall 01.01.2017-31.12.2024")
    public boolean isVirkningsdatoUgyldig() {
        return virkningsdato.isBefore(LocalDate.of(2017, 1, 1)) || virkningsdato.isAfter(LocalDate.of(2024, 12, 31));
    }
}
