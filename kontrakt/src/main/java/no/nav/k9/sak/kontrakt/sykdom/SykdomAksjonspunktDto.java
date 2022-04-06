package no.nav.k9.sak.kontrakt.sykdom;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SykdomAksjonspunktDto {

    @JsonProperty(value = "kanLøseAksjonspunkt")
    @Valid
    private final boolean kanLøseAksjonspunkt;

    @JsonProperty(value = "harUklassifiserteDokumenter")
    @Valid
    private final boolean harUklassifiserteDokumenter;

    @JsonProperty(value = "manglerDiagnosekode")
    @Valid
    private final boolean manglerDiagnosekode;

    @JsonProperty(value = "manglerGodkjentLegeerklæring")
    @Valid
    private final boolean manglerGodkjentLegeerklæring;

    @JsonProperty(value = "manglerVurderingAvKontinuerligTilsynOgPleie")
    @Valid
    private final boolean manglerVurderingAvKontinuerligTilsynOgPleie;

    @JsonProperty(value = "manglerVurderingAvToOmsorgspersoner")
    @Valid
    private final boolean manglerVurderingAvToOmsorgspersoner;

    @JsonProperty(value = "manglerVurderingAvILivetsSluttfase")
    @Valid
    private final boolean manglerVurderingAvILivetsSluttfase;

    @JsonProperty(value = "harDataSomIkkeHarBlittTattMedIBehandling")
    @Valid
    private final boolean harDataSomIkkeHarBlittTattMedIBehandling;

    @JsonProperty(value = "nyttDokumentHarIkkekontrollertEksisterendeVurderinger")
    @Valid
    private final boolean nyttDokumentHarIkkekontrollertEksisterendeVurderinger;

    @JsonCreator
    public SykdomAksjonspunktDto(@JsonProperty(value = "kanLøseAksjonspunkt") boolean kanLøseAksjonspunkt,
                                 @JsonProperty(value = "harUklassifiserteDokumenter") boolean harUklassifiserteDokumenter,
                                 @JsonProperty(value = "manglerDiagnosekode") boolean manglerDiagnosekode,
                                 @JsonProperty(value = "manglerGodkjentLegeerklæring") boolean manglerGodkjentLegeerklæring,
                                 @JsonProperty(value = "manglerVurderingAvKontinuerligTilsynOgPleie") boolean manglerVurderingAvKontinuerligTilsynOgPleie,
                                 @JsonProperty(value = "manglerVurderingAvToOmsorgspersoner") boolean manglerVurderingAvToOmsorgspersoner,
                                 @JsonProperty(value = "manglerVurderingAvILivetsSluttfase") boolean manglerVurderingAvILivetsSluttfase,
                                 @JsonProperty(value = "harDataSomIkkeHarBlittTattMedIBehandling") boolean harDataSomIkkeHarBlittTattMedIBehandling,
                                 @JsonProperty(value = "nyttDokumentHarIkkekontrollertEksisterendeVurderinger") boolean nyttDokumentHarIkkekontrollertEksisterendeVurderinger) {
        this.kanLøseAksjonspunkt = kanLøseAksjonspunkt;
        this.harUklassifiserteDokumenter = harUklassifiserteDokumenter;
        this.manglerDiagnosekode = manglerDiagnosekode;
        this.manglerGodkjentLegeerklæring = manglerGodkjentLegeerklæring;
        this.manglerVurderingAvKontinuerligTilsynOgPleie = manglerVurderingAvKontinuerligTilsynOgPleie;
        this.manglerVurderingAvToOmsorgspersoner = manglerVurderingAvToOmsorgspersoner;
        this.manglerVurderingAvILivetsSluttfase = manglerVurderingAvILivetsSluttfase;
        this.harDataSomIkkeHarBlittTattMedIBehandling = harDataSomIkkeHarBlittTattMedIBehandling;
        this.nyttDokumentHarIkkekontrollertEksisterendeVurderinger = nyttDokumentHarIkkekontrollertEksisterendeVurderinger;
    }

    public boolean isKanLøseAksjonspunkt() {
        return kanLøseAksjonspunkt;
    }

    public boolean isHarUklassifiserteDokumenter() {
        return harUklassifiserteDokumenter;
    }

    public boolean isManglerDiagnosekode() {
        return manglerDiagnosekode;
    }

    public boolean isManglerGodkjentLegeerklæring() {
        return manglerGodkjentLegeerklæring;
    }

    public boolean isManglerVurderingAvKontinuerligTilsynOgPleie() {
        return manglerVurderingAvKontinuerligTilsynOgPleie;
    }

    public boolean isManglerVurderingAvToOmsorgspersoner() {
        return manglerVurderingAvToOmsorgspersoner;
    }

    public boolean isManglerVurderingAvILivetsSluttfase() {
        return manglerVurderingAvILivetsSluttfase;
    }

    public boolean isHarDataSomIkkeHarBlittTattMedIBehandling() {
        return harDataSomIkkeHarBlittTattMedIBehandling;
    }
}
