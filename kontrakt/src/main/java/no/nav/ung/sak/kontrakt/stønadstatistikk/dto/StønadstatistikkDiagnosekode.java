package no.nav.ung.sak.kontrakt.stønadstatistikk.dto;

import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StønadstatistikkDiagnosekode {

    private static final String REGEXP = "^[A-Z][0-9][0-9a-z]*";
    private static final java.util.regex.Pattern PATTERN = java.util.regex.Pattern.compile(REGEXP);

    @JsonProperty(value = "kode", required = true)
    @NotNull
    @Pattern(regexp = REGEXP, message = "Diagnosekode [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String kode;

    @JsonProperty(value = "type", required = true)
    @NotNull
    @Valid
    private String type = "ICD10";


    protected StønadstatistikkDiagnosekode() {

    }

    public StønadstatistikkDiagnosekode(String kode) {
        if (!PATTERN.matcher(Objects.requireNonNull(kode, "kode")).matches()) {
            throw new IllegalArgumentException("Ugyldig kode:" + kode);
        }
        this.kode = Objects.requireNonNull(kode, "kode");
    }

    public String getKode() {
        return kode;
    }

    public String getType() {
        return type;
    }
}
