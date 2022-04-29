package no.nav.k9.sak.kontrakt.aksjonspunkt;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BekreftetOgOverstyrteAksjonspunkterDto {

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
    @Size(max = 10)
    private Collection<OverstyringAksjonspunktDto> overstyrteAksjonspunktDtoer = Collections.emptyList();

    @JsonProperty(value = "bekreftedeAksjonspunktDtoer")
    @Size(max = 10)
    @NotNull
    @Valid
    private Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer = Collections.emptyList();

    public static BekreftetOgOverstyrteAksjonspunkterDto lagDto(Long behandlingId, Long behandlingVersjon,
                                                                Collection<OverstyringAksjonspunktDto> overstyrteAksjonspunktDtoer,
                                                                Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer) {
        BekreftetOgOverstyrteAksjonspunkterDto dto = new BekreftetOgOverstyrteAksjonspunkterDto();
        dto.behandlingId = new BehandlingIdDto(behandlingId);
        dto.behandlingVersjon = behandlingVersjon;
        dto.overstyrteAksjonspunktDtoer = overstyrteAksjonspunktDtoer;
        dto.bekreftedeAksjonspunktDtoer = bekreftedeAksjonspunktDtoer;
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

    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }

    public Collection<OverstyringAksjonspunktDto> getOverstyrteAksjonspunktDtoer() {
        return Collections.unmodifiableCollection(overstyrteAksjonspunktDtoer);
    }

    public Collection<BekreftetAksjonspunktDto> getBekreftedeAksjonspunktDtoer() {
        return Collections.unmodifiableCollection(bekreftedeAksjonspunktDtoer);
    }

    @AbacAttributt("aksjonspunktKode")
    public Set<String> getAksjonspunktKoder() {
        var koder = overstyrteAksjonspunktDtoer.stream().map(OverstyringAksjonspunktDto::getKode).collect(Collectors.toCollection(HashSet::new));
        koder.addAll(bekreftedeAksjonspunktDtoer.stream().map(BekreftetAksjonspunktDto::getKode).collect(Collectors.toSet()));
        return koder;
    }

    public void setBehandlingId(BehandlingIdDto behandlingId) {
        this.behandlingId = behandlingId;
    }

    public void setBehandlingVersjon(Long behandlingVersjon) {
        this.behandlingVersjon = behandlingVersjon;
    }

    public void setOverstyrteAksjonspunktDtoer(Collection<OverstyringAksjonspunktDto> overstyrteAksjonspunktDtoer) {
        this.overstyrteAksjonspunktDtoer = List.copyOf(overstyrteAksjonspunktDtoer);
    }

    public void setBekreftedeAksjonspunktDtoer(Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer) {
        this.bekreftedeAksjonspunktDtoer = bekreftedeAksjonspunktDtoer;
    }

    @AssertTrue(message = "Må ha minst ett aksjonspunkt")
    public boolean minstEttAksjonspunkt() {
        return (bekreftedeAksjonspunktDtoer != null && bekreftedeAksjonspunktDtoer.size() > 0) || (overstyrteAksjonspunktDtoer != null && overstyrteAksjonspunktDtoer.size() > 0);
    }

}
