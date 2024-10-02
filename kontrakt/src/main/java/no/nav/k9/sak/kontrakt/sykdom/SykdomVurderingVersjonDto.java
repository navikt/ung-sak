package no.nav.k9.sak.kontrakt.sykdom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.sak.kontrakt.Patterns;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentDto;
import no.nav.k9.sak.typer.Periode;


// Sammensetning av SykdomVurdering og SykdomVurderingVersjon. For øverste nivå (dvs ikke de under "tidligereVersjoner") brukes SykdomVurderingVersjon med høyest versjon.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomVurderingVersjonDto {

    @JsonProperty(value = "versjon")
    @Size(max = 50)
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}-]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String versjon;

    @JsonProperty(value = "tekst")
    @Size(max = 4000)
    @Pattern(regexp = Patterns.FRITEKST, message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String tekst;

    @JsonProperty(value = "resultat")
    @Valid
    private Resultat resultat;

    @JsonProperty(value = "perioder")
    @Size(max = 100)
    @Valid
    private List<Periode> perioder = new ArrayList<>();

    // Liste av alle tilgjengelige dokumenter med markering av hvilke som har blitt valgt.
    @JsonProperty(value = "dokumenter")
    @Size(max = 100)
    @Valid
    private List<SykdomDokumentDto> dokumenter;

    /**
     * Saksbehandler eller automatisert prosess som har endret
     */
    @JsonProperty(value = "endretAv")
    @Size(max = 20)
    @Pattern(regexp = "^[\\p{Graph}\\p{Space}\\p{L}\\p{N}]+$", message = "[${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    private String endretAv;

    @JsonProperty(value = "endretTidspunkt")
    @Valid
    private LocalDateTime endretTidspunkt;



    public SykdomVurderingVersjonDto(String versjon,
                                     String tekst, Resultat resultat, List<Periode> perioder, List<SykdomDokumentDto> dokumenter, String endretAv,
                                     LocalDateTime endretTidspunkt) {
        this.versjon = versjon;
        this.tekst = tekst;
        this.resultat = resultat;
        this.perioder = perioder;
        this.dokumenter = dokumenter;
        this.endretAv = endretAv;
        this.endretTidspunkt = endretTidspunkt;
    }


}
