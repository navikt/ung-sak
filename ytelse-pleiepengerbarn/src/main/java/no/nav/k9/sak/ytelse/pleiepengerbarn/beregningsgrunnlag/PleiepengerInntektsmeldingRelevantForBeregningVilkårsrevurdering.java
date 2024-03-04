package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingRelevantForVilkårsrevurdering;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningResultat;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetForBeregningVurdering;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.OpptjeningsperioderTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Vurderer hvilke inntektsmeldinger som skal påvirke om beregningsvilkåret skal revurderes
 */
@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@VilkårTypeRef(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
public class PleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering implements InntektsmeldingRelevantForVilkårsrevurdering {

    private Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private OpptjeningRepository opptjeningRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    private boolean skalFiltrereBasertPåAktiviteter;


    public PleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering() {
    }

    @Inject
    public PleiepengerInntektsmeldingRelevantForBeregningVilkårsrevurdering(@Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning,
                                                                            InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                                            OpptjeningRepository opptjeningRepository,
                                                                            VilkårResultatRepository vilkårResultatRepository,
                                                                            @KonfigVerdi(value = "FORLENGELSE_IM_OPPTJENING_FILTER", defaultVerdi = "false") boolean skalFiltrereBasertPåAktiviteter) {
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.opptjeningRepository = opptjeningRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.skalFiltrereBasertPåAktiviteter = skalFiltrereBasertPåAktiviteter;
    }


    @Override
    public List<Inntektsmelding> begrensInntektsmeldinger(BehandlingReferanse referanse, Collection<Inntektsmelding> inntektsmeldinger, DatoIntervallEntitet periode) {
        var relevanteImTjeneste = InntektsmeldingerRelevantForBeregning.finnTjeneste(inntektsmeldingerRelevantForBeregning, referanse.getFagsakYtelseType());
        var inntektsmeldingBegrenset = relevanteImTjeneste.begrensSakInntektsmeldinger(referanse, inntektsmeldinger, periode);
        if (!skalFiltrereBasertPåAktiviteter) {
            return relevanteImTjeneste.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldingBegrenset, periode);
        } else {

            var opptjeningResultat = opptjeningRepository.finnOpptjening(referanse.getBehandlingId());
            var opptjening = opptjeningResultat.flatMap(it -> it.finnOpptjening(periode.getFomDato()));

            if (opptjeningResultat.isEmpty() || opptjening.isEmpty()) {
                return Collections.emptyList();
            }

            var erRelevantForBeregningVurderer = lagRelevansVurderer(referanse, periode, opptjeningResultat.get(), opptjening.get());
            var inntektsmeldingerTilBeregning = filtrerForBeregningsaktiviteter(inntektsmeldingBegrenset, erRelevantForBeregningVurderer);
            return relevanteImTjeneste.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldingerTilBeregning, periode);
        }
    }

    private ErIMRelevantForVilkårsvurdering lagRelevansVurderer(BehandlingReferanse referanse, DatoIntervallEntitet periode, OpptjeningResultat opptjeningResultat, Opptjening opptjening) {
        var vilkårUtfallMerknad = finnOpptjeningVilkårUtfallMerknad(referanse, periode);
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(referanse.getBehandlingId());
        return new ErIMRelevantForVilkårsvurdering(iayGrunnlag, new OpptjeningAktivitetForBeregningVurdering(opptjeningResultat), periode, referanse, vilkårUtfallMerknad, opptjening);
    }

    private VilkårUtfallMerknad finnOpptjeningVilkårUtfallMerknad(BehandlingReferanse referanse, DatoIntervallEntitet periode) {
        var opptjeningsvilkår = vilkårResultatRepository.hentHvisEksisterer(referanse.getBehandlingId()).flatMap(v -> v.getVilkår(VilkårType.OPPTJENINGSVILKÅRET));
        return opptjeningsvilkår.flatMap(v -> v.finnPeriodeForSkjæringstidspunktHvisFinnes(periode.getFomDato()))
            .map(VilkårPeriode::getMerknad)
            .orElse(null);
    }

    private List<Inntektsmelding> filtrerForBeregningsaktiviteter(Collection<Inntektsmelding> inntektsmeldingForPeriode, ErIMRelevantForVilkårsvurdering erRelevantForBeregningVurderer) {
        return inntektsmeldingForPeriode.stream().filter(erRelevantForBeregningVurderer::harGodkjentAktivitet).toList();

    }


}
