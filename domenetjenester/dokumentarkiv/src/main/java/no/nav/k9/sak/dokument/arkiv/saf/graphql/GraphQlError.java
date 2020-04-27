package no.nav.k9.sak.dokument.arkiv.saf.graphql;

import com.fasterxml.jackson.annotation.*;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GraphQlError {

    @JsonProperty("message")
    private String message;
    @JsonProperty("locations")
    private List<ErrorLocation> locations;
    @JsonProperty("path")
    private List<String> path;
    @JsonProperty("exceptionType")
    private String exceptionType;
    @JsonProperty("exception")
    private String exception;

    @JsonCreator
    public GraphQlError(@JsonProperty("message") String message,
                        @JsonProperty("locations") List<ErrorLocation> locations,
                        @JsonProperty("exceptionType") String exceptionType,
                        @JsonProperty("exception") String exception,
                        @JsonProperty("path") List<String> path) {
        this.message = message;
        this.locations = locations;
        this.exceptionType = exceptionType;
        this.exception = exception;
        this.path = path;
    }

    public String getMessage() {
        return message;
    }

    public List<ErrorLocation> getLocations() {
        return locations;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public String getException() {
        return exception;
    }

    public List<String> getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "GraphQlError{" +
                "message='" + message + '\'' +
                ", locations=" + locations +
                ", path=" + path +
                ", exceptionType='" + exceptionType + '\'' +
                ", exception='" + exception + '\'' +
                '}';
    }
}
