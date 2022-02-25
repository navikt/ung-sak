package no.nav.k9.sak.kontrakt.produksjonsstyring.los;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ProduksjonsstyringBehandlingHendelse extends ProduksjonsstyringHendelse {
    public final BehandlingStatus behandlingStatus;

    @JsonCreator
    public ProduksjonsstyringBehandlingHendelse(UUID eksternId,
                                                LocalDateTime hendelseTid,
                                                BehandlingStatus behandlingStatus) {
        super(eksternId, hendelseTid, K9SakHendelseType.BEHANDLING);
        this.behandlingStatus = behandlingStatus;
    }
}
