package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.skjæringstidspunkt.regelmodell;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Arbeidsforhold;

public class BeregningsgrunnlagPrStatus {
    private final AktivitetStatus aktivitetStatus;
    private final List<Arbeidsforhold> arbeidsforholdList;

    public BeregningsgrunnlagPrStatus(AktivitetStatus aktivitetStatus, Arbeidsforhold... arbeidsforhold) {
        this.aktivitetStatus = aktivitetStatus;
        this.arbeidsforholdList = (arbeidsforhold != null)
            ? Arrays.asList(arbeidsforhold).stream().filter(a -> a != null).collect(Collectors.toList())
            : Collections.emptyList();
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public List<Arbeidsforhold> getArbeidsforholdList() {
        return Collections.unmodifiableList(arbeidsforholdList);
    }
}
