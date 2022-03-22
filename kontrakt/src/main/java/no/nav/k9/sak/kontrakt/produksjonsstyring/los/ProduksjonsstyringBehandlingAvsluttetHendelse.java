package no.nav.k9.sak.kontrakt.produksjonsstyring.los;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ProduksjonsstyringBehandlingAvsluttetHendelse extends ProduksjonsstyringHendelse {
    public final String behandlingResultatType;

    @JsonCreator
    public ProduksjonsstyringBehandlingAvsluttetHendelse(
        @JsonProperty("eksternId") UUID eksternId,
        @JsonProperty("hendelseTid") LocalDateTime hendelseTid,
        @JsonProperty("behandlingResultatType") BehandlingResultatType behandlingResultatType) {
        super(eksternId, hendelseTid, K9SakHendelseType.BEHANDLING_AVSLUTTET);
        this.behandlingResultatType = behandlingResultatType.getKode();
    }
}
