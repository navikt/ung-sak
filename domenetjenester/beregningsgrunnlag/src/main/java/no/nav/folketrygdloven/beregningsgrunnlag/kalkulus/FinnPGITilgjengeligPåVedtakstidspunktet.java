package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.PGIPeriode;


@ApplicationScoped
public class FinnPGITilgjengeligPåVedtakstidspunktet {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private FagsakRepository fagsakRepository;

    public FinnPGITilgjengeligPåVedtakstidspunktet() {
    }

    @Inject
    public FinnPGITilgjengeligPåVedtakstidspunktet(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                   BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                                   FagsakRepository fagsakRepository) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.fagsakRepository = fagsakRepository;
    }

    public List<Inntekt> finnInntekter(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag forrigeInnhentet, LocalDate skjæringstidspunkt) {
        var inntektArbeidYtelseGrunnlag = finnIAYGrunnlag(behandlingReferanse, forrigeInnhentet, skjæringstidspunkt);
        var inntektFilter = new InntektFilter(inntektArbeidYtelseGrunnlag.getAktørInntektFraRegister(behandlingReferanse.getAktørId())).før(skjæringstidspunkt);
        return inntektFilter.getAlleInntektBeregnetSkatt();
    }

    private InntektArbeidYtelseGrunnlag finnIAYGrunnlag(BehandlingReferanse behandlingReferanse,
                                                        InntektArbeidYtelseGrunnlag forrigeInnhentet,
                                                        LocalDate skjæringstidspunkt) {
        var sigruninntektPeriode = beregningPerioderGrunnlagRepository.hentGrunnlag(behandlingReferanse.getBehandlingId()).stream()
            .flatMap(gr -> gr.getPGIPerioder().stream())
            .filter(p -> p.getSkjæringstidspunkt().equals(skjæringstidspunkt))
            .findFirst();
        var fagsak = fagsakRepository.finnEksaktFagsak(behandlingReferanse.getFagsakId());
        return sigruninntektPeriode.map(PGIPeriode::getIayReferanse)
            .map(iayRef -> inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(fagsak, iayRef))
            .orElse(forrigeInnhentet);
    }

}
