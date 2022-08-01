package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.prosess;

public class SykdomAksjonspunkt {

    private final boolean harUklassifiserteDokumenter;
    private final boolean manglerDiagnosekode;
    private final boolean manglerGodkjentLegeerklæring;
    private final boolean manglerVurderingAvKontinuerligTilsynOgPleie;
    private final boolean manglerVurderingAvToOmsorgspersoner;
    private final boolean manglerVurderingAvILivetsSluttfase;
    private final boolean harDataSomIkkeHarBlittTattMedIBehandling;
    private final boolean nyttDokumentHarIkkekontrollertEksisterendeVurderinger;


    public SykdomAksjonspunkt(boolean harUklassifiserteDokumenter,
                              boolean manglerDiagnosekode,
                              boolean manglerGodkjentLegeerklæring,
                              boolean manglerVurderingAvKontinuerligTilsynOgPleie,
                              boolean manglerVurderingAvToOmsorgspersoner,
                              boolean manglerVurderingAvILivetsSluttfase,
                              boolean harDataSomIkkeHarBlittTattMedIBehandling,
                              boolean nyttDokumentHarIkkekontrollertEksisterendeVurderinger) {
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
        /*
         * Merk at "nyttDokumentHarIkkekontrollertEksisterendeVurderinger" ikke er med i sjekken fordi
         * denne nå automatisk håndteres ved løsing av aksjonspunkt.
         */
        return !harUklassifiserteDokumenter &&
            !manglerDiagnosekode &&
            !manglerGodkjentLegeerklæring &&
            !manglerVurderingAvKontinuerligTilsynOgPleie &&
            !manglerVurderingAvToOmsorgspersoner &&
            !manglerVurderingAvILivetsSluttfase &&
            !nyttDokumentHarIkkekontrollertEksisterendeVurderinger;
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

    public boolean isManglerVurderingAvILivetsSluttfase() {
        return manglerVurderingAvILivetsSluttfase;
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

    public static SykdomAksjonspunkt bareFalse() {
        return new SykdomAksjonspunkt(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false);
    }
}
