package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.Resultat;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokument;


// Sammensetning av SykdomVurdering og SykdomVurderingVersjon. For øverste nivå (dvs ikke de under "tidligereVersjoner") brukes SykdomVurderingVersjon med høyest versjon.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomVurderingVersjonDto {

    @JsonProperty(value = "versjon")
    @Valid
    private String versjon;

    @JsonProperty(value = "tekst")
    @Valid
    private String tekst;

    @JsonProperty(value = "resultat")
    @Valid
    private Resultat resultat;

    @JsonProperty(value = "perioder")
    @Valid
    private List<Periode> perioder = new ArrayList<>();

    // Liste av alle tilgjengelige dokumenter med markering av hvilke som har blitt valgt.
    @JsonProperty(value = "dokumenter")
    @Valid
    private List<SykdomDokument> dokumenter;

    /**
     * Saksbehandler eller automatisert prosess som har endret
     */
    @JsonProperty(value = "endretAv")
    @Valid
    private String endretAv;

    @JsonProperty(value = "endretTidspunkt")
    @Valid
    private LocalDateTime endretTidspunkt;



    public SykdomVurderingVersjonDto(String versjon,
                                     String tekst, Resultat resultat, List<Periode> perioder, List<SykdomDokument> dokumenter, String endretAv,
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
