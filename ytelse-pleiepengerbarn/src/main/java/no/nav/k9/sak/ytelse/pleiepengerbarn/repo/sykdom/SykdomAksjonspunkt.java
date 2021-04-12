package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

public class SykdomAksjonspunkt {
    
    private final boolean harUklassifiserteDokumenter;
    private final boolean manglerDiagnosekode;
    private final boolean manglerGodkjentLegeerklæring;
    private final boolean manglerVurderingAvKontinuerligTilsynOgPleie;
    private final boolean manglerVurderingAvToOmsorgspersoner;
    private final boolean harDataSomIkkeHarBlittTattMedIBehandling;
    
    
    public SykdomAksjonspunkt(boolean harUklassifiserteDokumenter,
            boolean manglerDiagnosekode,
            boolean manglerGodkjentLegeerklæring,
            boolean manglerVurderingAvKontinuerligTilsynOgPleie,
            boolean manglerVurderingAvToOmsorgspersoner,
            boolean harDataSomIkkeHarBlittTattMedIBehandling) {
        this.harUklassifiserteDokumenter = harUklassifiserteDokumenter;
        this.manglerDiagnosekode = manglerDiagnosekode;
        this.manglerGodkjentLegeerklæring = manglerGodkjentLegeerklæring;
        this.manglerVurderingAvKontinuerligTilsynOgPleie = manglerVurderingAvKontinuerligTilsynOgPleie;
        this.manglerVurderingAvToOmsorgspersoner = manglerVurderingAvToOmsorgspersoner;
        this.harDataSomIkkeHarBlittTattMedIBehandling = harDataSomIkkeHarBlittTattMedIBehandling;
    }


    public boolean isKanLøseAksjonspunkt() {
        return !harUklassifiserteDokumenter && !manglerDiagnosekode && !manglerGodkjentLegeerklæring && !manglerVurderingAvKontinuerligTilsynOgPleie && !manglerVurderingAvToOmsorgspersoner;
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
    
    public boolean isHarDataSomIkkeHarBlittTattMedIBehandling() {
        return harDataSomIkkeHarBlittTattMedIBehandling;
    }
}
