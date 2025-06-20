package no.nav.ung.sak.kontrakt.etterlysning;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.ung.abac.AbacAttributt;
import no.nav.ung.sak.kontrakt.behandling.BehandlingIdDto;

import java.util.List;

public class EndreFristRequest {

    /**
     * Ekstern-referanse for etterlysning og ny frist
     */
    @Valid
    @NotNull
    @Size(min = 1, max = 20)
    private List<EndreFristDto> endretFrister;


    @JsonProperty(value = "behandlingId", required = true)
    @NotNull
    @Valid
    private BehandlingIdDto behandlingId;

    @NotNull
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingVersjon;

    public EndreFristRequest(List<EndreFristDto> endretFrister, BehandlingIdDto behandlingId, Long behandlingVersjon) {
        this.endretFrister = endretFrister;
        this.behandlingId = behandlingId;
        this.behandlingVersjon = behandlingVersjon;
    }

    protected EndreFristRequest() {
        //
    }

    @AbacAttributt("behandlingId")
    public Long getBehandlingId() {
        return behandlingId.getBehandlingId();
    }

    public Long getBehandlingVersjon() {
        return behandlingVersjon;
    }

    public List<EndreFristDto> getEndretFrister() {
        return endretFrister;
    }


    @Override
    public String toString() {
        return "EndreFristForEtterlysningDto{" +
            "endretFrister=" + endretFrister +
            ", behandlingId=" + behandlingId +
            ", behandlingVersjon=" + behandlingVersjon +
            '}';
    }
}
