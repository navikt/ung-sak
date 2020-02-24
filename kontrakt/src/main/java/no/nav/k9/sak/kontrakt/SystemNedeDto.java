package no.nav.k9.sak.kontrakt;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class SystemNedeDto {

    @JsonProperty(value = "endepunkt")
    @Size(max = 500)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{P}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String endepunkt;

    @JsonProperty(value = "feilmelding")
    @Size(max = 50000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{P}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String feilmelding;

    @JsonProperty(value = "nedeFremTilTidspunkt")
    private LocalDateTime nedeFremTilTidspunkt;

    @JsonProperty(value = "stackTrace")
    @Size(max = 50000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{P}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String stackTrace;

    @JsonProperty(value = "systemNavn", required = true)
    @NotNull
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{P}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String systemNavn;

    public SystemNedeDto(String systemNavn, String endepunkt, LocalDateTime nedeFremTilTidspunkt, String feilmelding, String stackTrace) {
        this.systemNavn = systemNavn;
        this.endepunkt = endepunkt;
        this.nedeFremTilTidspunkt = nedeFremTilTidspunkt;
        this.feilmelding = feilmelding;
        this.stackTrace = stackTrace;
    }

    protected SystemNedeDto() {
        //
    }

    public String getEndepunkt() {
        return endepunkt;
    }

    public String getFeilmelding() {
        return feilmelding;
    }

    public LocalDateTime getNedeFremTilTidspunkt() {
        return nedeFremTilTidspunkt;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public String getSystemNavn() {
        return systemNavn;
    }
}
