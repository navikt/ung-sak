package no.nav.k9.sak.dokument.arkiv.saf.graphql;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GraphQlRequest {

    @JsonProperty("query")
    private String query;
    @JsonProperty("variables")
    private Variables variables;

    @JsonCreator
    public GraphQlRequest(@JsonProperty("query") String query, @JsonProperty("variables") Variables variables) {
        this.query = query;
        this.variables = variables;
    }

    public String getQuery() {
        return query;
    }

    public Variables getVariables() {
        return variables;
    }

}
