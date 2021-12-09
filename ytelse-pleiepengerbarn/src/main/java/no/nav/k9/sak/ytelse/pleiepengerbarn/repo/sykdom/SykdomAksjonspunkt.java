package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

public class SykdomAksjonspunkt {

    private final boolean harUklassifiserteDokumenter;
    private final boolean manglerDiagnosekode;
    private final boolean manglerGodkjentLegeerklæring;
    private final boolean manglerVurderingAvKontinuerligTilsynOgPleie;
    private final boolean manglerVurderingAvToOmsorgspersoner;
    private final boolean harDataSomIkkeHarBlittTattMedIBehandling;
    private final boolean nyttDokumentHarIkkekontrollertEksisterendeVurderinger;


    public SykdomAksjonspunkt(boolean harUklassifiserteDokumenter,
                              boolean manglerDiagnosekode,
                              boolean manglerGodkjentLegeerklæring,
                              boolean manglerVurderingAvKontinuerligTilsynOgPleie,
                              boolean manglerVurderingAvToOmsorgspersoner,
                              boolean harDataSomIkkeHarBlittTattMedIBehandling,
                              boolean nyttDokumentHarIkkekontrollertEksisterendeVurderinger) {
        this.harUklassifiserteDokumenter = harUklassifiserteDokumenter;
        this.manglerDiagnosekode = manglerDiagnosekode;
        this.manglerGodkjentLegeerklæring = manglerGodkjentLegeerklæring;
        this.manglerVurderingAvKontinuerligTilsynOgPleie = manglerVurderingAvKontinuerligTilsynOgPleie;
        this.manglerVurderingAvToOmsorgspersoner = manglerVurderingAvToOmsorgspersoner;
        this.harDataSomIkkeHarBlittTattMedIBehandling = harDataSomIkkeHarBlittTattMedIBehandling;
        this.nyttDokumentHarIkkekontrollertEksisterendeVurderinger = nyttDokumentHarIkkekontrollertEksisterendeVurderinger;
    }


    public boolean isKanLøseAksjonspunkt() {
        /*
         * Merk at "nyttDokumentHarIkkekontrollertEksisterendeVurderinger" ikke er med i sjekken fordi
         * denne nå automatisk håndteres ved løsing av aksjonspunkt.
         */
        return !harUklassifiserteDokumenter &&
            !manglerDiagnosekode &&
            !manglerGodkjentLegeerklæring &&
            !manglerVurderingAvKontinuerligTilsynOgPleie &&
            !manglerVurderingAvToOmsorgspersoner;
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

    public boolean isNyttDokumentHarIkkekontrollertEksisterendeVurderinger() {
        return nyttDokumentHarIkkekontrollertEksisterendeVurderinger;
    }
}
