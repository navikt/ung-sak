package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.NæringsinntektPeriode;


@ApplicationScoped
public class NæringsinntektFilter {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider;

    public NæringsinntektFilter() {
    }

    @Inject
    public NæringsinntektFilter(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.oppgittOpptjeningFilterProvider = oppgittOpptjeningFilterProvider;
    }

    public List<Inntekt> finnInntekter(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag forrigeInnhentet, LocalDate skjæringstidspunkt) {
        var oppgittOpptjeningFilter = oppgittOpptjeningFilterProvider.finnOpptjeningFilter(behandlingReferanse.getBehandlingId());
        var inntektArbeidYtelseGrunnlag = finnIAYGrunnlag(behandlingReferanse, forrigeInnhentet, skjæringstidspunkt, oppgittOpptjeningFilter);
        var inntektFilter = new InntektFilter(inntektArbeidYtelseGrunnlag.getAktørInntektFraRegister(behandlingReferanse.getAktørId())).før(skjæringstidspunkt);
        return inntektFilter.getAlleInntektBeregnetSkatt();
    }

    private InntektArbeidYtelseGrunnlag finnIAYGrunnlag(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag forrigeInnhentet, LocalDate skjæringstidspunkt, OppgittOpptjeningFilter oppgittOpptjeningFilter) {
        if (erSelvstendigNæringsdrivende(behandlingReferanse, forrigeInnhentet, oppgittOpptjeningFilter, skjæringstidspunkt)) {
            var næringsinntektPeriode = beregningPerioderGrunnlagRepository.hentGrunnlag(behandlingReferanse.getBehandlingId()).stream()
                .flatMap(gr -> gr.getNæringsinntektPerioder().stream())
                .filter(p -> p.getSkjæringstidspunkt().equals(skjæringstidspunkt))
                .findFirst();
            return næringsinntektPeriode.map(NæringsinntektPeriode::getIayReferanse)
                .map(iayRef -> inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingReferanse.getBehandlingId(), iayRef))
                .orElse(forrigeInnhentet);
        }
        return forrigeInnhentet;
    }


    private boolean erSelvstendigNæringsdrivende(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, OppgittOpptjeningFilter oppgittOpptjeningFilter, LocalDate stp) {
        return oppgittOpptjeningFilter.hentOppgittOpptjening(ref.getBehandlingId(), iayGrunnlag, stp).stream()
            .flatMap(oo -> oo.getEgenNæring().stream())
            .anyMatch(e -> e.getPeriode().inkluderer(stp.minusDays(1)));
    }

}
