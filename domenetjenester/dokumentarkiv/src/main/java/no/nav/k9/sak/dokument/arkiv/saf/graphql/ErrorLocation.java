package no.nav.k9.sak.dokument.arkiv.saf.graphql;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ErrorLocation {

    @JsonProperty("line")
    private String line;
    @JsonProperty("column")
    private String column;

    @JsonCreator
    public ErrorLocation(@JsonProperty("line") String line,
                         @JsonProperty("column") String column) {
        this.line = line;
        this.column = column;
    }

    public String getLine() {
        return line;
    }

    public String getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return "ErrorLocation{" +
                "line='" + line + '\'' +
                ", column='" + column + '\'' +
                '}';
    }
}
