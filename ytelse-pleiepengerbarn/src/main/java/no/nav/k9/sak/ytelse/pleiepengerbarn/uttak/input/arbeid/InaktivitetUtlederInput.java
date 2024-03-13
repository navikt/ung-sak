package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.util.List;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.typer.AktørId;

public class InaktivitetUtlederInput {

    private final boolean skalKjøreNyLogikkForSpeiling;
    private AktørId brukerAktørId;
    private LocalDateTimeline<Boolean> tidslinjeTilVurdering;
    private InntektArbeidYtelseGrunnlag iayGrunnlag;
    private List<Beregningsgrunnlag> beregningsgrunnlag;



    public InaktivitetUtlederInput(AktørId brukerAktørId, LocalDateTimeline<Boolean> tidslinjeTilVurdering, InntektArbeidYtelseGrunnlag iayGrunnlag, boolean skalKjøreNyLogikkForSpeiling, List<Beregningsgrunnlag> beregningsgrunnlag) {
        this.brukerAktørId = brukerAktørId;
        this.tidslinjeTilVurdering = tidslinjeTilVurdering;
        this.iayGrunnlag = iayGrunnlag;
        this.skalKjøreNyLogikkForSpeiling = skalKjøreNyLogikkForSpeiling;
        this.beregningsgrunnlag = beregningsgrunnlag;
    }

    public List<Beregningsgrunnlag> getBeregningsgrunnlag() {
        return beregningsgrunnlag;
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

    public boolean skalKjøreNyLogikkForSpeiling() {
        return skalKjøreNyLogikkForSpeiling;
    }


}
