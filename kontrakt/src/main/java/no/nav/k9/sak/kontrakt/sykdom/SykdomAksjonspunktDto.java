package no.nav.k9.sak.kontrakt.sykdom;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
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
    private boolean kanLøseAksjonspunkt;

    @JsonProperty(value = "harUklassifiserteDokumenter")
    @Valid
    private boolean harUklassifiserteDokumenter;

    @JsonProperty(value = "manglerDiagnosekode")
    @Valid
    private boolean manglerDiagnosekode;

    @JsonProperty(value = "manglerGodkjentLegeerklæring")
    @Valid
    private boolean manglerGodkjentLegeerklæring;

    @JsonProperty(value = "manglerVurderingAvKontinuerligTilsynOgPleie")
    @Valid
    private boolean manglerVurderingAvKontinuerligTilsynOgPleie;

    @JsonProperty(value = "manglerVurderingAvToOmsorgspersoner")
    @Valid
    private boolean manglerVurderingAvToOmsorgspersoner;

    @JsonProperty(value = "manglerVurderingAvILivetsSluttfase")
    @Valid
    private boolean manglerVurderingAvILivetsSluttfase;

    @JsonProperty(value = "harDataSomIkkeHarBlittTattMedIBehandling")
    @Valid
    private boolean harDataSomIkkeHarBlittTattMedIBehandling;

    @JsonProperty(value = "nyttDokumentHarIkkekontrollertEksisterendeVurderinger")
    @Valid
    private boolean nyttDokumentHarIkkekontrollertEksisterendeVurderinger;

    public SykdomAksjonspunktDto() {
    }

    public SykdomAksjonspunktDto(boolean kanLøseAksjonspunkt,
                                 boolean harUklassifiserteDokumenter,
                                 boolean manglerDiagnosekode,
                                 boolean manglerGodkjentLegeerklæring,
                                 boolean manglerVurderingAvKontinuerligTilsynOgPleie,
                                 boolean manglerVurderingAvToOmsorgspersoner,
                                 boolean manglerVurderingAvILivetsSluttfase,
                                 boolean harDataSomIkkeHarBlittTattMedIBehandling,
                                 boolean nyttDokumentHarIkkekontrollertEksisterendeVurderinger) {
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
