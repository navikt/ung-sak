package no.nav.k9.sak.kontrakt.behandling;

import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.abac.AbacAttributt;
import no.nav.k9.sak.typer.Saksnummer;

/**
 * Referanse til en behandling.
 * Enten {@link #behandlingId} eller {@link #behandlingUuid} vil være satt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonInclude(Include.NON_NULL)
public class BehandlingIdDto {

    @JsonProperty(value = "saksnummer")
    @Valid
    private Saksnummer saksnummer;

    @JsonProperty(value = "behandlingId")
    @Min(0)
    @Max(Long.MAX_VALUE)
    private Long behandlingId;

    /**
     * Behandling UUID (nytt alternativ til intern behandlingId. Bør brukes av eksterne systemer).
     */
    @JsonProperty(value = "behandlingUuid")
    @Valid
    private UUID behandlingUuid;

    public BehandlingIdDto() {
        //
    }

    /**
     * Default ctor for å instantiere med en type id. Støtter både Long id og UUID.
     */
    public BehandlingIdDto(String id) {
        Objects.requireNonNull(id, "behandlingId");
        if (id.contains("-")) {
            this.behandlingUuid = UUID.fromString(id);
        } else {
            this.behandlingId = Long.valueOf(id);
        }
    }

    public BehandlingIdDto(BehandlingUuidDto uuidDto) {
        this.behandlingUuid = uuidDto.getBehandlingUuid();
    }

    public BehandlingIdDto(Saksnummer saksnummer, Long behandlingId, UUID behandlingUuid) {
        this.saksnummer = saksnummer;
        this.behandlingId = behandlingId;
        this.behandlingUuid = behandlingUuid;
    }

    @AssertTrue(message = "Ikke spesifier både behandlingId og behandlingUuid")
    private boolean ok() {
        return (behandlingId == null || behandlingUuid == null) && !(behandlingId == null && behandlingUuid == null);
    }

    public BehandlingIdDto(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    /**
     * Denne er kun intern nøkkel, bør ikke eksponeres ut men foreløpig støttes både Long id og UUID id for behandling på grensesnittene.
     */
    @AbacAttributt("behandlingId")
    public Long getBehandlingId() {
        return behandlingId;
    }

    @AbacAttributt("behandlingUuid")
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    @AbacAttributt("saksnummer")
    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '<' +
            (saksnummer == null ? "" : "saksnummer=" + saksnummer + ", ") +
            (behandlingId != null ? "behandlingId=" + behandlingId : "") +
            (behandlingId != null && behandlingUuid != null ? ", " : "") +
            (behandlingUuid != null ? "behandlingUuid=" + behandlingUuid : "") +
            '>';
    }
}
