package no.nav.ung.domenetjenester.arkiv.dok.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KnyttTilAnnenSakRequest {

    @JsonProperty("sakstype")
    private Sakstype sakstype;

    @JsonProperty("fagsakId")
    private String fagsakId;

    @JsonProperty("fagsaksystem")
    private String fagsaksystem;

    @JsonProperty("tema")
    private String tema;

    @JsonProperty("bruker")
    private Bruker bruker;

    @JsonProperty("journalfoerendeEnhet")
    private String journalfoerendeEnhet;


    public KnyttTilAnnenSakRequest() {

    }

    public KnyttTilAnnenSakRequest(Sakstype sakstype, String fagsakId, String fagsaksystem, String tema, Bruker bruker,
            String journalfoerendeEnhet) {
        this.sakstype = Objects.requireNonNull(sakstype, "sakstype");
        if (sakstype == Sakstype.FAGSAK) {
            this.fagsakId = Objects.requireNonNull(fagsakId, "fagsakId");
            this.fagsaksystem = Objects.requireNonNull(fagsaksystem, "fagsaksystem");
        }
        this.tema = Objects.requireNonNull(tema, "tema");
        this.bruker = Objects.requireNonNull(bruker, "bruker");
        this.journalfoerendeEnhet = Objects.requireNonNull(journalfoerendeEnhet, "journalfoerendeEnhet");
    }


    public Sakstype getSakstype() {
        return sakstype;
    }

    public String getFagsakId() {
        return fagsakId;
    }

    public String getFagsaksystem() {
        return fagsaksystem;
    }
    public String getTema() {
        return tema;
    }

    public Bruker getBruker() {
        return bruker;
    }

    public String getJournalfoerendeEnhet() {
        return journalfoerendeEnhet;
    }
}
