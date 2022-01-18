package no.nav.k9.sak.kontrakt.sykdom.dokument;

import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomDokumentUtkvitterEksisterendeVurderingerDto {

    @JsonProperty(value = "behandlingUuid", required = true)
    @Valid
    @NotNull
    private UUID behandlingUuid;

    @JsonProperty(value = "dokumenterSomSkalUtkvitteres")
    @Size(max = 100)
    @Valid
    private List<String> dokumenterSomSkalUtkvitteres;

    public SykdomDokumentUtkvitterEksisterendeVurderingerDto() {

    }

    public SykdomDokumentUtkvitterEksisterendeVurderingerDto(UUID behandlingUuid, List<String> dokumenterSomSkalUtkvitteres) {
        this.behandlingUuid = behandlingUuid;
        this.dokumenterSomSkalUtkvitteres = dokumenterSomSkalUtkvitteres;
    }

    @AbacAttributt("behandlingUuid")
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public List<String> getDokumenterSomSkalUtkvitteres() {
        return dokumenterSomSkalUtkvitteres;
    }
}
