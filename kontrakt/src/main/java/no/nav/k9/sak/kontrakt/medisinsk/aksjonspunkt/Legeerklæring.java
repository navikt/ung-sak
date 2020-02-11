package no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Legeerklæring {

    @JsonProperty(value = "fom", required = true)
    @Valid
    @NotNull
    private LocalDate fom;

    // NULL i tom tilsier at den er løpende til neste legeerklæring kommer
    @JsonProperty(value = "tom")
    @Valid
    private LocalDate tom;

    @JsonProperty(value = "identifikator")
    @Valid
    private UUID identifikator;

    @JsonProperty(value = "kilde", required = true)
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String kilde;

    @JsonProperty(value = "diagnosekode", required = true)
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String diagnosekode;

    @JsonProperty(value = "innleggelsesperioder")
    @Valid
    @Size(max = 100)
    private List<Periode> innleggelsesperioder;

    private Legeerklæring() {
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public String getDiagnosekode() {
        return diagnosekode;
    }

    public String getKilde() {
        return kilde;
    }

    public List<Periode> getInnleggelsesperioder() {
        return innleggelsesperioder;
    }

    public UUID getIdentifikator() {
        return identifikator;
    }
}
