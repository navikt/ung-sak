package no.nav.k9.sak.kontrakt.tilsyn;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.List;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeredskapDto {

    @JsonProperty(value = "beskrivelser")
    @Size(max = 1000)
    @Valid
    private List<BeskrivelseDto> beskrivelser;

    @JsonProperty(value = "vurderinger")
    @Size(max = 1000)
    @Valid
    private List<VurderingDto> vurderinger;

    public BeredskapDto(List<BeskrivelseDto> beskrivelser, List<VurderingDto> vurderinger) {
        this.beskrivelser = beskrivelser;
        this.vurderinger = vurderinger;
    }

    public List<BeskrivelseDto> getBeskrivelser() {
        return beskrivelser;
    }

    public List<VurderingDto> getVurderinger() {
        return vurderinger;
    }

}
