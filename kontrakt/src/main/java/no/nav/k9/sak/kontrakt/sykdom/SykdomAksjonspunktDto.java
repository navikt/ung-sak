package no.nav.k9.sak.kontrakt.sykdom;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    
    public SykdomAksjonspunktDto(boolean kanLøseAksjonspunkt,
            boolean harUklassifiserteDokumenter,
            boolean manglerDiagnosekode,
            boolean manglerGodkjentLegeerklæring,
            boolean manglerVurderingAvKontinuerligTilsynOgPleie,
            boolean manglerVurderingAvToOmsorgspersoner) {
        this.kanLøseAksjonspunkt = kanLøseAksjonspunkt;
        this.harUklassifiserteDokumenter = harUklassifiserteDokumenter;
        this.manglerDiagnosekode = manglerDiagnosekode;
        this.manglerGodkjentLegeerklæring = manglerGodkjentLegeerklæring;
        this.manglerVurderingAvKontinuerligTilsynOgPleie = manglerVurderingAvKontinuerligTilsynOgPleie;
        this.manglerVurderingAvToOmsorgspersoner = manglerVurderingAvToOmsorgspersoner;
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
}