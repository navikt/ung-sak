package no.nav.k9.sak.registerendringer;

import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class ArbeidsgiverEndring {

    private Arbeidsgiver arbeidsgiver;
    private LocalDateTimeline<Set<Endringstype>> endringerForUtbetaling;
    private Set<Endringstype> endringerForVilkårsvurdering;

    public ArbeidsgiverEndring(Arbeidsgiver arbeidsgiver,
                               LocalDateTimeline<Set<Endringstype>> endringerForUtbetaling,
                               Set<Endringstype> endringerForVilkårsvurdering) {
        this.arbeidsgiver = arbeidsgiver;
        this.endringerForUtbetaling = endringerForUtbetaling;
        this.endringerForVilkårsvurdering = endringerForVilkårsvurdering;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public LocalDateTimeline<Set<Endringstype>> getEndringerForUtbetaling() {
        return endringerForUtbetaling;
    }

    public Set<Endringstype> getEndringerForVilkårsvurdering() {
        return endringerForVilkårsvurdering;
    }
}
