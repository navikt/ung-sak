package no.nav.ung.sak.kontrakt.produksjonsstyring.los;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "hendelseType")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSubTypes(value = {
        @JsonSubTypes.Type(name = "KRAVDOKUMENT", value = ProduksjonsstyringDokumentHendelse.class),
        @JsonSubTypes.Type(name = "AKSJONSPUNKT", value = ProduksjonsstyringAksjonspunktHendelse.class),
        @JsonSubTypes.Type(name = "BEHANDLING_OPPRETTET", value = ProduksjonsstyringBehandlingOpprettetHendelse.class),
        @JsonSubTypes.Type(name = "BEHANDLING_AVSLUTTET", value = ProduksjonsstyringBehandlingAvsluttetHendelse.class),
})
public abstract class ProduksjonsstyringHendelse {
    public final UUID eksternId;
    public final LocalDateTime hendelseTid;
    public final UngSakHendelseType hendelseType;

    @JsonCreator
    public ProduksjonsstyringHendelse(UUID eksternId, LocalDateTime hendelseTid, UngSakHendelseType hendelseType) {
        this.eksternId = eksternId;
        this.hendelseTid = hendelseTid;
        this.hendelseType = hendelseType;
    }

    public String tryggToString() {
        return "ProduksjonsstyringHendelse{" +
                "EksternId: " + eksternId + ", " +
                "HendelseType: " + hendelseType + ", " +
                "HendelseTid: " + hendelseTid +
            "}";
    }
}
