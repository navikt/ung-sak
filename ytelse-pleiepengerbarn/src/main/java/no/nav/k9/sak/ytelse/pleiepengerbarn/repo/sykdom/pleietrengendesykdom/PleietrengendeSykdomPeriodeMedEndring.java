package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom;

import java.util.Objects;

import no.nav.k9.sak.typer.Periode;


public class PleietrengendeSykdomPeriodeMedEndring {

    private Periode periode;

    private boolean endrerVurderingSammeBehandling;

    private boolean endrerAnnenVurdering;

    private PleietrengendeSykdomVurderingVersjon gammelVersjon;


    public PleietrengendeSykdomPeriodeMedEndring(Periode periode, boolean endrerVurderingSammeBehandling, boolean endrerAnnenVurdering, PleietrengendeSykdomVurderingVersjon gammelVersjon) {
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

    public PleietrengendeSykdomVurderingVersjon getGammelVersjon() {
        return gammelVersjon;
    }
}
