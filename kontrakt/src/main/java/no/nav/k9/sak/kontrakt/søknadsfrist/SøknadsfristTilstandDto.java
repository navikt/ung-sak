package no.nav.k9.sak.kontrakt.søknadsfrist;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SøknadsfristTilstandDto {

    @Valid
    @Size
    @NotNull
    @JsonProperty("dokumentStatus")
    private List<KravDokumentStatus> kravDokumentStatus;

    @JsonCreator
    public SøknadsfristTilstandDto(@Valid
                                   @Size
                                   @NotNull
                                   @JsonProperty(value = "dokumentStatus", required = true) List<KravDokumentStatus> kravDokumentStatus) {
        this.kravDokumentStatus = kravDokumentStatus;
    }

    public List<KravDokumentStatus> getKravDokumentStatus() {
        return kravDokumentStatus;
    }
}
