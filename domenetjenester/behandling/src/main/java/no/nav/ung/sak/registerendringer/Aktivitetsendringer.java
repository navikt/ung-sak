package no.nav.ung.sak.registerendringer;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;

public class Aktivitetsendringer {

    private Arbeidsgiver arbeidsgiver;

    private InternArbeidsforholdRef arbeidsforholdRef;
    private LocalDateTimeline<Endringstype> endringerForUtbetaling;

    public Aktivitetsendringer(Arbeidsgiver arbeidsgiver,
                               InternArbeidsforholdRef arbeidsforholdRef, LocalDateTimeline<Endringstype> endringerForUtbetaling) {
        this.arbeidsgiver = arbeidsgiver;
        this.arbeidsforholdRef = arbeidsforholdRef;
        this.endringerForUtbetaling = endringerForUtbetaling;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public InternArbeidsforholdRef getArbeidsforholdRef() {
        return arbeidsforholdRef;
    }

    public LocalDateTimeline<Endringstype> getEndringerForUtbetaling() {
        return endringerForUtbetaling;
    }
}
