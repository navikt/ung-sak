package no.nav.folketrygdloven.beregningsgrunnlag.input;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.opptjening.OpptjeningAktiviteter.OpptjeningPeriode;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.AktørId;

/** Inputstruktur for beregningsgrunnlag tjenester. */
public class BeregningsgrunnlagInput {

    /** Aktiviteter for graderign av uttak. */
    private final AktivitetGradering aktivitetGradering;

    /** Data som referer behandlingen beregningsgrunnlag inngår i. */
    private BehandlingReferanse behandlingReferanse;

    /** Grunnlag for Beregningsgrunnlg opprettet eller modifisert av modulen. Settes på av modulen. */
    private BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlag;

    /** IAY grunnlag benyttet av beregningsgrunnlag. Merk kan bli modifisert av innhenting av inntekter for beregning, sammenligning. */
    private final InntektArbeidYtelseGrunnlag iayGrunnlag;

    /** Aktiviteter til grunnlag for opptjening. */
    private final OpptjeningAktiviteter opptjeningAktiviteter;

    private final YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag;

    public BeregningsgrunnlagInput(BehandlingReferanse behandlingReferanse,
                                   InntektArbeidYtelseGrunnlag iayGrunnlag,
                                   OpptjeningAktiviteter opptjeningAktiviteter,
                                   AktivitetGradering aktivitetGradering,
                                   YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag) {
        this.behandlingReferanse = Objects.requireNonNull(behandlingReferanse, "behandlingReferanse");
        this.iayGrunnlag = iayGrunnlag;
        this.opptjeningAktiviteter = opptjeningAktiviteter;
        this.aktivitetGradering = aktivitetGradering;
        this.ytelsespesifiktGrunnlag = ytelsespesifiktGrunnlag;
    }

    private BeregningsgrunnlagInput(BeregningsgrunnlagInput input) {
        this(input.getBehandlingReferanse(), input.getIayGrunnlag(), input.getOpptjeningAktiviteter(), input.getAktivitetGradering(), input.getYtelsespesifiktGrunnlag());
        this.beregningsgrunnlagGrunnlag = input.getBeregningsgrunnlagGrunnlag();
    }

    public AktivitetGradering getAktivitetGradering() {
        return aktivitetGradering;
    }

    public AktørId getAktørId() {
        return behandlingReferanse.getAktørId();
    }

    public BehandlingReferanse getBehandlingReferanse() {
        return behandlingReferanse;
    }

    public BeregningsgrunnlagGrunnlagEntitet getBeregningsgrunnlagGrunnlag() {
        return beregningsgrunnlagGrunnlag;
    }

    public BeregningsgrunnlagEntitet getBeregningsgrunnlag() {
        return beregningsgrunnlagGrunnlag == null ? null : beregningsgrunnlagGrunnlag.getBeregningsgrunnlag().orElseThrow();
    }

    public FagsakYtelseType getFagsakYtelseType() {
        return behandlingReferanse.getFagsakYtelseType();
    }

    public InntektArbeidYtelseGrunnlag getIayGrunnlag() {
        return iayGrunnlag;
    }

    public Collection<Inntektsmelding> getInntektsmeldinger() {
        LocalDate skjæringstidspunktOpptjening = getSkjæringstidspunktOpptjening();
        if(skjæringstidspunktOpptjening == null) return Collections.emptyList();
        return new InntektsmeldingFilter(iayGrunnlag).hentInntektsmeldingerBeregning(getBehandlingReferanse(), skjæringstidspunktOpptjening);
    }

    public OpptjeningAktiviteter getOpptjeningAktiviteter() {
        return opptjeningAktiviteter;
    }

    public Collection<OpptjeningPeriode> getOpptjeningAktiviteterForBeregning() {
        LocalDate skjæringstidspunktOpptjening = getSkjæringstidspunktOpptjening();
        if(skjæringstidspunktOpptjening == null) return Collections.emptyList();
        var aktivitetFilter = new OpptjeningsaktiviteterPerYtelse(getFagsakYtelseType());
        var relevanteAktiviteter = opptjeningAktiviteter.getOpptjeningPerioder()
            .stream()
            .filter(p -> {
                return p.getPeriode().getFom().isBefore(skjæringstidspunktOpptjening);
            })
            .filter(p -> aktivitetFilter.erRelevantAktivitet(p.getOpptjeningAktivitetType()))
            .collect(Collectors.toList());
        return relevanteAktiviteter;
    }

    public Skjæringstidspunkt getSkjæringstidspunkt() {
        return behandlingReferanse.getSkjæringstidspunkt();
    }

    public LocalDate getSkjæringstidspunktForBeregning() {
        return behandlingReferanse.getSkjæringstidspunktBeregning();
    }

    public LocalDate getSkjæringstidspunktOpptjening() {
        return getSkjæringstidspunkt().getSkjæringstidspunktOpptjening();
    }

    /** Sjekk fagsakytelsetype før denne kalles. */
    @SuppressWarnings("unchecked")
    public <V extends YtelsespesifiktGrunnlag> V getYtelsespesifiktGrunnlag() {
        return (V) ytelsespesifiktGrunnlag;
    }

    public BeregningsgrunnlagInput medBeregningsgrunnlagGrunnlag(BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        var newInput = new BeregningsgrunnlagInput(this);
        newInput.beregningsgrunnlagGrunnlag = grunnlag;
        newInput = grunnlag.getBeregningsgrunnlag()
            .map(BeregningsgrunnlagEntitet::getSkjæringstidspunkt)
            .map(newInput::medSkjæringstidspunktForBeregning)
            .orElse(newInput);
        return newInput;
    }

    /** Overstyrer behandlingreferanse, eks for å få ny skjæringstidspunkt fra beregningsgrunnlag fra tidligere. */
    public BeregningsgrunnlagInput medBehandlingReferanse(BehandlingReferanse ref) {
        var newInput = new BeregningsgrunnlagInput(this);
        newInput.behandlingReferanse = Objects.requireNonNull(ref, "behandlingReferanse");
        return newInput;
    }

    private BeregningsgrunnlagInput medSkjæringstidspunktForBeregning(LocalDate skjæringstidspunkt) {
        var newInput = new BeregningsgrunnlagInput(this);
        var nyttSkjæringstidspunkt = Skjæringstidspunkt.builder(this.behandlingReferanse.getSkjæringstidspunkt()).medSkjæringstidspunktBeregning(skjæringstidspunkt).build();
        newInput.behandlingReferanse = this.behandlingReferanse.medSkjæringstidspunkt(nyttSkjæringstidspunkt);
        return newInput;
    }

}
