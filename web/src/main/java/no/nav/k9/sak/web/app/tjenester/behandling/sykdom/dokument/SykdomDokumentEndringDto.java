package no.nav.k9.sak.web.app.tjenester.behandling.sykdom.dokument;

import java.time.LocalDate;
import java.util.Objects;
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
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomDokumentEndringDto {

    @JsonProperty(value = "behandlingUuid", required = true)
    @Valid
    @NotNull
    private UUID behandlingUuid;
    
    @JsonProperty(value = "id", required = true)
    @Size(max = 50)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String id;
    
    @JsonProperty(value = "versjon")
    @Size(max = 50)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String versjon;
    
    @JsonProperty(value = "type", required = true)
    @Valid
    @NotNull
    private SykdomDokumentType type;
    
    @JsonProperty(value = "datert", required = true)
    @Valid
    @NotNull
    private LocalDate datert;

    
    public SykdomDokumentEndringDto() {
        
    }
    
    public SykdomDokumentEndringDto(String behandlingUuid) {
        this.behandlingUuid = UUID.fromString(behandlingUuid);
    }
    
    public SykdomDokumentEndringDto(UUID behandlingUuid) {
        this.behandlingUuid = Objects.requireNonNull(behandlingUuid);
    }
    
    
    @AbacAttributt("behandlingUuid")
    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }
    
    public String getId() {
        return id;
    }

    public String getVersjon() {
        return versjon;
    }

    public SykdomDokumentType getType() {
        return type;
    }
    
    public LocalDate getDatert() {
        return datert;
    }
}