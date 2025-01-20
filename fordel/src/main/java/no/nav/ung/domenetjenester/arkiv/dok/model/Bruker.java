package no.nav.ung.domenetjenester.arkiv.dok.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Bruker {

    @JsonProperty("id")
    private String id;
    @JsonProperty("idType")
    private BrukerIdType idType;

    @JsonCreator
    public Bruker(@JsonProperty("id") String id, @JsonProperty("idType") BrukerIdType type) {
        this.id = id;
        this.idType = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BrukerIdType getIdType() {
        return idType;
    }

    public boolean erAktoerId() {
        return BrukerIdType.AKTOERID.equals(idType);
    }
}
