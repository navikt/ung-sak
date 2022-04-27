package no.nav.k9.sak.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.kontrakt.behandling.BehandlingIdDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = JsonInclude.Include.NON_ABSENT, content = JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class DeaktiverSkjæringstidspunktDto {


    @JsonProperty(value = "skjæringstidspunkt", required = true)
    @NotNull
    @Valid
    private LocalDate skjæringstidspunkt;

    @JsonProperty(value = "behandlingId", required = true)
    @NotNull
    @Valid
    private BehandlingIdDto behandlingId;


    public DeaktiverSkjæringstidspunktDto(LocalDate skjæringstidspunkt, BehandlingIdDto behandlingId) {
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.behandlingId = behandlingId;
    }

    public DeaktiverSkjæringstidspunktDto() {
    }

    @AbacAttributt("behandlingId")
    public Long getBehandlingId() {
        return behandlingId.getBehandlingId();
    }

    @AbacAttributt("behandlingUuid")
    public UUID getBehandlingUuid() {
        return behandlingId.getBehandlingUuid();
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }
}
