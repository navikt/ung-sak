package no.nav.k9.sak.web.app.tjenester.behandling.sykdom.dokument;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.kontrakt.ResourceLink;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomDiagnosekoderDto {

    @JsonProperty(value = "behandlingUuid", required = true)
    @Valid
    private UUID behandlingUuid;

    /**
     * Versjonen man tok utgangspunkt i før endring.
     */
    @JsonProperty(value = "versjon")
    @Size(max = 50)
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String versjon;

    @JsonProperty(value = "diagnosekoder")
    @Size(max = 1000)
    @Valid
    private List<SykdomDiagnosekodeDto> diagnosekoder = new ArrayList<>();

    @JsonProperty(value = "links")
    @Size(max = 100)
    @Valid
    private List<ResourceLink> links = new ArrayList<>();

    public SykdomDiagnosekoderDto() {

    }

    public SykdomDiagnosekoderDto(String behandlingUuid) {
        this.behandlingUuid = UUID.fromString(behandlingUuid);
    }

    public SykdomDiagnosekoderDto(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
    }

    public SykdomDiagnosekoderDto(UUID behandlingUuid, String versjon, List<SykdomDiagnosekodeDto> diagnosekoder, List<ResourceLink> links) {
        this.behandlingUuid = behandlingUuid;
        this.versjon = versjon;
        this.diagnosekoder = diagnosekoder;
        this.links = links;
    }


    @AbacAttributt("behandlingUuid")
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public String getVersjon() {
        return versjon;
    }

    public List<SykdomDiagnosekodeDto> getDiagnosekoder() {
        return diagnosekoder;
    }
}
