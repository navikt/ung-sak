package no.nav.k9.sak.kontrakt.behandling;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

//TODO (TOR) Denne skal returnere rettigheter for behandlingsmeny i klient.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class BehandlingRettigheterDto {

    @JsonProperty(value="harSoknad")
    private Boolean harSoknad;

    @JsonCreator
    public BehandlingRettigheterDto(@JsonProperty(value="harSoknad") Boolean harSoknad) {
        this.harSoknad = harSoknad;
    }

    public Boolean getHarSoknad() {
        return harSoknad;
    }
}
