package no.nav.k9.sak.dokument.arkiv.saf.graphql;

import com.fasterxml.jackson.annotation.*;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GraphQlResponse {

    @JsonProperty("data")
    private GrapQlData data;
    @JsonProperty("errors")
    private List<GraphQlError> errors;

    @JsonCreator
    public GraphQlResponse(@JsonProperty("data") GrapQlData data, @JsonProperty("errors") List<GraphQlError> errors) {
        this.data = data;
        this.errors = errors;
    }

    public GrapQlData getData() {
        return data;
    }

    public List<GraphQlError> getErrors() {
        return errors;
    }
}
