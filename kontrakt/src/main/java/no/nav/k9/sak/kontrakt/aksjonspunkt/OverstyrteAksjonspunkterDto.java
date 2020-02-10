package no.nav.k9.sak.kontrakt.aksjonspunkt;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OverstyrteAksjonspunkterDto {

    @JsonProperty(value = "behandlingId", required = true)
    @NotNull
    @Valid
    private BehandlingIdDto behandlingId;

    @JsonAlias("versjon")
    @JsonProperty(value = "behandlingVersjon", required = true)
    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingVersjon;

    @JsonProperty(value = "overstyrteAksjonspunktDtoer")
    @Valid
    @Size(min = 1, max = 10)
    private Collection<OverstyringAksjonspunktDto> overstyrteAksjonspunktDtoer;

    public static OverstyrteAksjonspunkterDto lagDto(Long behandlingId, Long behandlingVersjon,
                                                     Collection<OverstyringAksjonspunktDto> overstyrteAksjonspunktDtoer) {
        OverstyrteAksjonspunkterDto dto = new OverstyrteAksjonspunkterDto();
        dto.behandlingId = new BehandlingIdDto(behandlingId);
        dto.behandlingVersjon = behandlingVersjon;
        dto.overstyrteAksjonspunktDtoer = overstyrteAksjonspunktDtoer;
        return dto;
    }

    @AbacAttributt("behandlingId")
    public Long getBehandlingId() {
        return behandlingId.getBehandlingId();
    }

    @AbacAttributt("behandlingUuid")
    public UUID getBehandlingUuid() {
        return behandlingId.getBehandlingUuid();
    }

    @AbacAttributt("aksjonspunktKode")
    public Set<String> getOverstyrteAksjonspunktKoder() {
        return overstyrteAksjonspunktDtoer.stream().map(OverstyringAksjonspunktDto::getKode).collect(Collectors.toSet());
    }

    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }

    public Collection<OverstyringAksjonspunktDto> getOverstyrteAksjonspunktDtoer() {
        return overstyrteAksjonspunktDtoer;
    }

}
