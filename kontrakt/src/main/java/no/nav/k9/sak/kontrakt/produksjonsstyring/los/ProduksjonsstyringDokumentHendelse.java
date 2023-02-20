package no.nav.k9.sak.kontrakt.produksjonsstyring.los;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.krav.KravDokumentType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ProduksjonsstyringDokumentHendelse extends ProduksjonsstyringHendelse {
    public final List<KravDokumentType> kravdokumenter;

    @JsonCreator
    public ProduksjonsstyringDokumentHendelse(
        @JsonProperty("eksternId") UUID eksternId,
        @JsonProperty("hendelseTid") LocalDateTime hendelseTid,
        @JsonProperty("kravdokumenter") List<KravDokumentType> kravdokumenter) {
        super(eksternId, hendelseTid, K9SakHendelseType.KRAVDOKUMENT);
        this.kravdokumenter = kravdokumenter;
    }
}
