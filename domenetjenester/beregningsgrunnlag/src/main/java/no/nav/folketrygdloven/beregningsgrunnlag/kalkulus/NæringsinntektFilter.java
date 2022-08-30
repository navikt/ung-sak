package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.NæringsinntektPeriode;


public class NæringsinntektFilter {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;

    public NæringsinntektFilter() {
    }

    @Inject
    public NæringsinntektFilter(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste, BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
    }

    public List<Inntekt> finnInntekter(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag forrigeInnhentet, LocalDate skjæringstidspunkt) {
        var næringsinntektPeriode = beregningPerioderGrunnlagRepository.hentGrunnlag(behandlingReferanse.getBehandlingId()).stream()
            .flatMap(gr -> gr.getNæringsinntektPerioder().stream())
            .filter(p -> p.getSkjæringstidspunkt().equals(skjæringstidspunkt))
            .findFirst();
        var inntektArbeidYtelseGrunnlag = næringsinntektPeriode.map(NæringsinntektPeriode::getIayReferanse)
            .map(iayRef -> inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(behandlingReferanse.getBehandlingId(), iayRef))
            .orElse(forrigeInnhentet);
        var inntektFilter = new InntektFilter(inntektArbeidYtelseGrunnlag.getAktørInntektFraRegister(behandlingReferanse.getAktørId())).før(skjæringstidspunkt);
        return inntektFilter.getAlleInntektBeregnetSkatt();
    }

}
