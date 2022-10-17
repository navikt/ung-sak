package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.PGIPeriode;


@ApplicationScoped
public class FinnPGITilgjengeligPåVedtakstidspunktet {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider;
    private FagsakRepository fagsakRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    public FinnPGITilgjengeligPåVedtakstidspunktet() {
    }

    @Inject
    public FinnPGITilgjengeligPåVedtakstidspunktet(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                   BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                                   OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider,
                                                   FagsakRepository fagsakRepository,
                                                   VilkårResultatRepository vilkårResultatRepository) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.oppgittOpptjeningFilterProvider = oppgittOpptjeningFilterProvider;
        this.fagsakRepository = fagsakRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public List<Inntekt> finnInntekter(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag forrigeInnhentet, LocalDate skjæringstidspunkt) {
        var oppgittOpptjeningFilter = oppgittOpptjeningFilterProvider.finnOpptjeningFilter(behandlingReferanse.getBehandlingId());
        var inntektArbeidYtelseGrunnlag = finnIAYGrunnlag(behandlingReferanse, forrigeInnhentet, skjæringstidspunkt, oppgittOpptjeningFilter);
        var inntektFilter = new InntektFilter(inntektArbeidYtelseGrunnlag.getAktørInntektFraRegister(behandlingReferanse.getAktørId())).før(skjæringstidspunkt);
        return inntektFilter.getAlleInntektBeregnetSkatt();
    }

    private InntektArbeidYtelseGrunnlag finnIAYGrunnlag(BehandlingReferanse behandlingReferanse,
                                                        InntektArbeidYtelseGrunnlag forrigeInnhentet,
                                                        LocalDate skjæringstidspunkt,
                                                        OppgittOpptjeningFilter oppgittOpptjeningFilter) {
        if (erSelvstendigNæringsdrivende(behandlingReferanse, forrigeInnhentet, oppgittOpptjeningFilter, skjæringstidspunkt) || erMidlertidigInaktiv(behandlingReferanse, skjæringstidspunkt)) {
            var sigruninntektPeriode = beregningPerioderGrunnlagRepository.hentGrunnlag(behandlingReferanse.getBehandlingId()).stream()
                .flatMap(gr -> gr.getPGIPerioder().stream())
                .filter(p -> p.getSkjæringstidspunkt().equals(skjæringstidspunkt))
                .findFirst();
            var fagsak = fagsakRepository.finnEksaktFagsak(behandlingReferanse.getFagsakId());
            return sigruninntektPeriode.map(PGIPeriode::getIayReferanse)
                .map(iayRef -> inntektArbeidYtelseTjeneste.hentGrunnlagForGrunnlagId(fagsak, iayRef))
                .orElse(forrigeInnhentet);
        }
        return forrigeInnhentet;
    }

    private boolean erMidlertidigInaktiv(BehandlingReferanse behandlingReferanse, LocalDate skjæringstidspunkt) {
        var opptjeningsvilkår = vilkårResultatRepository.hent(behandlingReferanse.getBehandlingId()).getVilkår(VilkårType.OPPTJENINGSVILKÅRET);
        var vilkårUtfallMerknad = finnVilkårmerknadForOpptjening(opptjeningsvilkår, skjæringstidspunkt);
        return VilkårUtfallMerknad.VM_7847_A.equals(vilkårUtfallMerknad) || VilkårUtfallMerknad.VM_7847_B.equals(vilkårUtfallMerknad);
    }


    private boolean erSelvstendigNæringsdrivende(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, OppgittOpptjeningFilter oppgittOpptjeningFilter, LocalDate stp) {
        return oppgittOpptjeningFilter.hentOppgittOpptjening(ref.getBehandlingId(), iayGrunnlag, stp).stream()
            .flatMap(oo -> oo.getEgenNæring().stream())
            .anyMatch(e -> e.getPeriode().inkluderer(stp.minusDays(1)));
    }

    private VilkårUtfallMerknad finnVilkårmerknadForOpptjening(Optional<Vilkår> opptjeningsvilkår, LocalDate skjæringstidspunkt) {
        VilkårUtfallMerknad vilkårsMerknad = null;
        if (opptjeningsvilkår.isPresent()) {
            vilkårsMerknad = opptjeningsvilkår.get().finnPeriodeForSkjæringstidspunkt(skjæringstidspunkt).getMerknad();
        }
        return vilkårsMerknad;
    }

}
