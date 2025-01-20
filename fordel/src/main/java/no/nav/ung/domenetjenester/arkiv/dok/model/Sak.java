package no.nav.ung.domenetjenester.arkiv.dok.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Sak {

    @JsonProperty("fagsaksystem")
    private String fagsaksystem;
    @JsonProperty("fagsakId")
    private String fagsakId;
    @JsonProperty("sakstype")
    private Sakstype sakstype;

    @JsonCreator
    public Sak(@JsonProperty("fagsaksystem") String fagsaksystem, @JsonProperty("fagsakId") String fagsakId, @JsonProperty("sakstype") Sakstype sakstype) {
        this.fagsaksystem = fagsaksystem;
        this.fagsakId = fagsakId;
        this.sakstype = sakstype;
    }

    public String getFagsaksystem() {
        return fagsaksystem;
    }

    public String getFagsakId() {
        return fagsakId;
    }

    public Sakstype getSakstype() {
        return sakstype;
    }

    @Override
    public String toString() {
        return "Sak{" +
                "arkivsaksystem='" + fagsaksystem + '\'' +
                ", arkivsaksnummer='" + fagsakId + '\'' +
                ", sakstype='" + sakstype + '\'' +
                '}';
    }
}
