package no.nav.k9.sak.kontrakt.medisinsk.aksjonspunkt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.medisinsk.LegeerklæringKilde;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Legeerklæring {

    @JsonProperty(value = "diagnosekode", required = true)
    @Size(max = 4000)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{Sc}\\p{L}\\p{M}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    private String diagnosekode;

    @JsonProperty(value = "fom", required = true)
    @Valid
    @NotNull
    private LocalDate fom;

    @JsonProperty(value = "identifikator")
    @Valid
    private UUID identifikator;

    @JsonProperty(value = "innleggelsesperioder")
    @Valid
    @Size(max = 100)
    private List<Periode> innleggelsesperioder;

    @JsonProperty(value = "kilde", required = true)
    @Valid
    @NotNull
    private LegeerklæringKilde kilde;

    // NULL i tom tilsier at den er løpende til neste legeerklæring kommer
    @JsonProperty(value = "tom")
    @Valid
    private LocalDate tom;

    public Legeerklæring(LocalDate fom, LocalDate tom,
                         UUID identifikator,
                         LegeerklæringKilde kilde, String diagnosekode, List<Periode> innleggelsesperioder) {
        this.fom = Objects.requireNonNull(fom, "fom");
        this.tom = Objects.requireNonNull(tom, "tom");
        this.identifikator = identifikator == null ? UUID.randomUUID() : identifikator;
        this.kilde = Objects.requireNonNull(kilde, "kilde");
        this.diagnosekode = diagnosekode;
        this.innleggelsesperioder = innleggelsesperioder == null ? new ArrayList<>() : List.copyOf(innleggelsesperioder);
    }

    protected Legeerklæring() {
        //
    }

    public String getDiagnosekode() {
        return diagnosekode;
    }

    public void setDiagnosekode(String diagnosekode) {
        this.diagnosekode = diagnosekode;
    }

    public LocalDate getFom() {
        return fom;
    }

    public void setFom(LocalDate fom) {
        this.fom = fom;
    }

    public UUID getIdentifikator() {
        return identifikator;
    }

    public void setIdentifikator(UUID identifikator) {
        this.identifikator = identifikator;
    }

    public List<Periode> getInnleggelsesperioder() {
        if (innleggelsesperioder == null) {
            return List.of();
        }
        return Collections.unmodifiableList(innleggelsesperioder);
    }

    public void setInnleggelsesperioder(List<Periode> innleggelsesperioder) {
        this.innleggelsesperioder = List.copyOf(innleggelsesperioder);
    }

    public LegeerklæringKilde getKilde() {
        return kilde;
    }

    public void setKilde(LegeerklæringKilde kilde) {
        this.kilde = kilde;
    }

    public LocalDate getTom() {
        return tom;
    }

    public void setTom(LocalDate tom) {
        this.tom = tom;
    }
}
