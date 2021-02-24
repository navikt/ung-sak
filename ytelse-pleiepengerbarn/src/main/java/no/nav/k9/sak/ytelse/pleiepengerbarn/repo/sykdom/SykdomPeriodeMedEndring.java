package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.util.Objects;

import no.nav.k9.sak.typer.Periode;


public class SykdomPeriodeMedEndring {

    private Periode periode;

    private boolean endrerVurderingSammeBehandling;

    private boolean endrerAnnenVurdering;
    
    private SykdomVurderingVersjon gammelVersjon;


    public SykdomPeriodeMedEndring(Periode periode, boolean endrerVurderingSammeBehandling, boolean endrerAnnenVurdering, SykdomVurderingVersjon gammelVersjon) {
        this.periode = Objects.requireNonNull(periode, "periode");
        this.endrerVurderingSammeBehandling = endrerVurderingSammeBehandling;
        this.endrerAnnenVurdering = endrerAnnenVurdering;
        this.gammelVersjon = Objects.requireNonNull(gammelVersjon, "gammelVersjon");
    }

    
    public Periode getPeriode() {
        return periode;
    }
    
    public boolean isEndrerAnnenVurdering() {
        return endrerAnnenVurdering;
    }
    
    public boolean isEndrerVurderingSammeBehandling() {
        return endrerVurderingSammeBehandling;
    }
    
    public SykdomVurderingVersjon getGammelVersjon() {
        return gammelVersjon;
    }
}
