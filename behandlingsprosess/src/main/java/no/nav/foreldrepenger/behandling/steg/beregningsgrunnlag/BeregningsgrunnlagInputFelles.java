package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.input.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;


public abstract class BeregningsgrunnlagInputFelles {

    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private AndelGraderingTjeneste andelGraderingTjeneste;
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;

    @Inject
    public BeregningsgrunnlagInputFelles(BehandlingRepository behandlingRepository,
                                         InntektArbeidYtelseTjeneste iayTjeneste,
                                         SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                         AndelGraderingTjeneste andelGraderingTjeneste,
                                         OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste) {
        this.behandlingRepository = Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.skjæringstidspunktTjeneste = Objects.requireNonNull(skjæringstidspunktTjeneste, "skjæringstidspunktTjeneste");
        this.andelGraderingTjeneste = Objects.requireNonNull(andelGraderingTjeneste, "andelGrderingTjeneste");
        this.opptjeningForBeregningTjeneste = Objects.requireNonNull(opptjeningForBeregningTjeneste, "opptjeningForBeregningTjeneste");
    }

    protected BeregningsgrunnlagInputFelles() {
        // for CDI proxy
    }

    public BeregningsgrunnlagInput lagInput(Behandling behandling) {
        var behandlingId = behandling.getId();
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);

        return lagInput(ref, iayGrunnlag).orElseThrow();
    }

    public Optional<BeregningsgrunnlagInput> lagInput(Behandling behandling, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return lagInput(ref, iayGrunnlag);
    }

    /** Returnerer input hvis data er på tilgjengelig for det, ellers Optional.empty(). */
    private Optional<BeregningsgrunnlagInput> lagInput(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var aktivitetGradering = andelGraderingTjeneste.utled(ref);
        var opptjeningAktiviteter = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(ref, iayGrunnlag);

        if (opptjeningAktiviteter.isEmpty()) {
            return Optional.empty();
        }

        var ytelseGrunnlag = getYtelsespesifiktGrunnlag(ref);
        return Optional.of(new BeregningsgrunnlagInput(ref, iayGrunnlag, opptjeningAktiviteter.orElseThrow(), aktivitetGradering, ytelseGrunnlag));
    }

    /** Returnerer input hvis data er på tilgjengelig for det, ellers Optional.empty(). */
    public BeregningsgrunnlagInput lagInput(Long behandlingId) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return lagInput(behandling, iayGrunnlag).orElseThrow();
    }

    public abstract YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref);
}
