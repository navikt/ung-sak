package no.nav.k9.sak.kontrakt.produksjonsstyring.los;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.aksjonspunkt.AksjonspunktTilstandDto;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ProduksjonsstyringAksjonspunktHendelse extends ProduksjonsstyringHendelse {
    public final List<AksjonspunktTilstandDto> aksjonspunktTilstander;

    @JsonCreator
    public ProduksjonsstyringAksjonspunktHendelse(@JsonProperty("eksternId") UUID eksternId,
                                                  @JsonProperty("hendelseTid") LocalDateTime hendelseTid,
                                                  @JsonProperty("aksjonspunktTilstander") List<AksjonspunktTilstandDto> aksjonspunktTilstander
    ) {
        super(eksternId, hendelseTid, K9SakHendelseType.AKSJONSPUNKT);
        this.aksjonspunktTilstander = aksjonspunktTilstander;
    }
}
