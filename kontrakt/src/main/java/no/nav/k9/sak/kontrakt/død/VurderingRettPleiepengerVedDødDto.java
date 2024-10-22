package no.nav.k9.sak.kontrakt.død;

import com.fasterxml.jackson.annotation.*;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.kodeverk.uttak.RettVedDødType;
import no.nav.k9.sak.kontrakt.Patterns;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_RETT_ETTER_PLEIETRENGENDES_DØD)
public class VurderingRettPleiepengerVedDødDto extends BekreftetAksjonspunktDto {

    @JsonProperty(value = "vurdering")
    @Size(max = 4096)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String vurdering;

    @JsonProperty(value = "rettVedDødType")
    private RettVedDødType rettVedDødType;

    @JsonProperty(value = "vurdertAv")
    @Pattern(regexp = Patterns.BOKSTAVER_OG_TALL, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String vurdertAv;

    @JsonProperty(value = "vurdertTidspunkt")
    @Valid
    private LocalDateTime vurdertTidspunkt;

    public VurderingRettPleiepengerVedDødDto() {
        //
    }

    public VurderingRettPleiepengerVedDødDto(String vurdering, RettVedDødType rettVedDødType, String vurdertAv, LocalDateTime vurdertTidspunkt) {
        this.vurdering = vurdering;
        this.rettVedDødType = rettVedDødType;
        this.vurdertAv = vurdertAv;
        this.vurdertTidspunkt = vurdertTidspunkt;
    }

    public String getVurdering() {
        return vurdering;
    }

    public RettVedDødType getRettVedDødType() {
        return rettVedDødType;
    }

}
