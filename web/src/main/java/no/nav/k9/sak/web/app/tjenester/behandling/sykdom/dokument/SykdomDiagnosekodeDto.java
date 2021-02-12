package no.nav.k9.sak.web.app.tjenester.behandling.sykdom.dokument;

import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonFormat(shape = JsonFormat.Shape.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class SykdomDiagnosekodeDto {

    private static final String REGEXP = "^[A-Z][0-9][0-9a-z]*";
    private static final java.util.regex.Pattern PATTERN = java.util.regex.Pattern.compile(REGEXP);

    @JsonValue
    @NotNull
    @Pattern(regexp = REGEXP, message = "Diagnosekode [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String diagnosekode;

    protected SykdomDiagnosekodeDto() {
        //
    }

    @JsonCreator
    public SykdomDiagnosekodeDto(String diagnosekode) {
        if (!PATTERN.matcher(Objects.requireNonNull(diagnosekode, "diagnosekode")).matches()) {
            throw new IllegalArgumentException("Ugyldig diagnosekode:" + diagnosekode);
        }
        this.diagnosekode = diagnosekode;
    }
    
    public String getVerdi() {
        return diagnosekode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((diagnosekode == null) ? 0 : diagnosekode.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SykdomDiagnosekodeDto other = (SykdomDiagnosekodeDto) obj;
        if (diagnosekode == null) {
            if (other.diagnosekode != null)
                return false;
        } else if (!diagnosekode.equals(other.diagnosekode))
            return false;
        return true;
    }
}
