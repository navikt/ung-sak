package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.typer.AktørId;

public class InaktivitetUtlederInput {

    private AktørId brukerAktørId;
    private LocalDateTimeline<Boolean> tidslinjeTilVurdering;
    private InntektArbeidYtelseGrunnlag iayGrunnlag;

    public InaktivitetUtlederInput(AktørId brukerAktørId, LocalDateTimeline<Boolean> tidslinjeTilVurdering, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        this.brukerAktørId = brukerAktørId;
        this.tidslinjeTilVurdering = tidslinjeTilVurdering;
        this.iayGrunnlag = iayGrunnlag;
    }

    public InntektArbeidYtelseGrunnlag getIayGrunnlag() {
        return iayGrunnlag;
    }

    public LocalDateTimeline<Boolean> getTidslinjeTilVurdering() {
        return tidslinjeTilVurdering;
    }

    public AktørId getBrukerAktørId() {
        return brukerAktørId;
    }
}
