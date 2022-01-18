package no.nav.k9.sak.kontrakt.behandling;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SakRettigheterDto {

    @JsonProperty(value = "sakSkalTilInfotrygd")
    private boolean sakSkalTilInfotrygd;

    @JsonProperty(value = "behandlingTypeKanOpprettes")
    @Valid
    @Size(max = 10)
    private List<BehandlingOpprettingDto> behandlingTypeKanOpprettes;

    @JsonProperty(value = "behandlingTillatteOperasjoner")
    @Valid
    @Size(max = 100)
    private List<BehandlingOperasjonerDto> behandlingTillatteOperasjoner;

    public SakRettigheterDto(boolean sakSkalTilInfotrygd, List<BehandlingOpprettingDto> behandlingTypeKanOpprettes, List<BehandlingOperasjonerDto> behandlingTillatteOperasjoner) {
        this.sakSkalTilInfotrygd = sakSkalTilInfotrygd;
        this.behandlingTypeKanOpprettes = behandlingTypeKanOpprettes;
        this.behandlingTillatteOperasjoner = behandlingTillatteOperasjoner;
    }

    public boolean isSakSkalTilInfotrygd() {
        return sakSkalTilInfotrygd;
    }

    public List<BehandlingOpprettingDto> getBehandlingTypeKanOpprettes() {
        return behandlingTypeKanOpprettes;
    }

    public List<BehandlingOperasjonerDto> getBehandlingTillatteOperasjoner() {
        return behandlingTillatteOperasjoner;
    }
}
