package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

public class InaktivitetUtlederInput {

    private final boolean skalKjøreNyLogikkForSpeiling;
    private AktørId brukerAktørId;
    private LocalDateTimeline<Boolean> tidslinjeTilVurdering;
    private InntektArbeidYtelseGrunnlag iayGrunnlag;


    public InaktivitetUtlederInput(AktørId brukerAktørId, LocalDateTimeline<Boolean> tidslinjeTilVurdering, InntektArbeidYtelseGrunnlag iayGrunnlag, boolean skalKjøreNyLogikkForSpeiling) {
        this.brukerAktørId = brukerAktørId;
        this.tidslinjeTilVurdering = tidslinjeTilVurdering;
        this.iayGrunnlag = iayGrunnlag;
        this.skalKjøreNyLogikkForSpeiling = skalKjøreNyLogikkForSpeiling;
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
