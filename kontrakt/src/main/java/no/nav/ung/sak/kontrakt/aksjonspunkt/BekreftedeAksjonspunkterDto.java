package no.nav.ung.sak.kontrakt.aksjonspunkt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.abac.StandardAbacAttributt;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BekreftedeAksjonspunkterDto {

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

    @JsonProperty(value = "bekreftedeAksjonspunktDtoer", required = true)
    @Size(min = 1, max = 10)
    @NotNull
    @Valid
    private Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer = Collections.emptyList();

    public static BekreftedeAksjonspunkterDto lagDto(Long behandlingId, Long behandlingVersjon,
                                                     Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer) {
        BekreftedeAksjonspunkterDto dto = new BekreftedeAksjonspunkterDto();
        dto.behandlingId = new BehandlingIdDto(behandlingId);
        dto.behandlingVersjon = behandlingVersjon;
        dto.bekreftedeAksjonspunktDtoer = bekreftedeAksjonspunktDtoer;
        return dto;
    }

    @StandardAbacAttributt(StandardAbacAttributtType.BEHANDLING_ID)
    public Long getBehandlingId() {
        return behandlingId.getBehandlingId();
    }

    @StandardAbacAttributt(StandardAbacAttributtType.BEHANDLING_UUID)
    public UUID getBehandlingUuid() {
        return behandlingId.getBehandlingUuid();
    }

    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }

    public Collection<BekreftetAksjonspunktDto> getBekreftedeAksjonspunktDtoer() {
        return Collections.unmodifiableCollection(bekreftedeAksjonspunktDtoer);
    }

    @StandardAbacAttributt(StandardAbacAttributtType.AKSJONSPUNKT_KODE)
    public Set<String> getBekreftedeAksjonspunktKoder() {
        return bekreftedeAksjonspunktDtoer.stream().map(BekreftetAksjonspunktDto::getKode).collect(Collectors.toSet());
    }

    public void setBehandlingVersjon(Long behandlingVersjon) {
        this.behandlingVersjon = behandlingVersjon;
    }

    public void setBekreftedeAksjonspunktDtoer(Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer) {
        this.bekreftedeAksjonspunktDtoer = List.copyOf(bekreftedeAksjonspunktDtoer);
    }

}
