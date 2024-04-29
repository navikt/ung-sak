package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static no.nav.k9.sak.domene.opptjening.aksjonspunkt.MapYrkesaktivitetTilOpptjeningsperiodeTjeneste.mapYrkesaktivitet;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurdering;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.aksjonspunkt.MapYtelsesstidslinjerForPermisjonvalidering;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class ErIMRelevantForVilkårsvurdering {

    private final InntektArbeidYtelseGrunnlag iayGrunnlag;
    private final OpptjeningAktivitetVurdering utfallVurderer;
    private final DatoIntervallEntitet vilkårsperiode;
    private final Map<OpptjeningAktivitetType, LocalDateTimeline<Boolean>> tidslinjePrYtelse;
    private final BehandlingReferanse behandlingReferanse;
    private final VilkårUtfallMerknad vilkårUtfallMerknad;
    private final Opptjening opptjening;

    ErIMRelevantForVilkårsvurdering(InntektArbeidYtelseGrunnlag iayGrunnlag,
                                    OpptjeningAktivitetVurdering utfallVurderer,
                                    DatoIntervallEntitet vilkårsperiode,
                                    BehandlingReferanse referanse,
                                    VilkårUtfallMerknad vilkårUtfallMerknad,
                                    Opptjening opptjening) {
        this.iayGrunnlag = iayGrunnlag;
        this.utfallVurderer = utfallVurderer;
        this.vilkårsperiode = vilkårsperiode;
        this.vilkårUtfallMerknad = vilkårUtfallMerknad;
        this.opptjening = opptjening;
        this.tidslinjePrYtelse = new MapYtelsesstidslinjerForPermisjonvalidering().utledYtelsesTidslinjerForValideringAvPermisjoner(new YtelseFilter(iayGrunnlag.getAktørYtelseFraRegister(referanse.getAktørId())));
        this.behandlingReferanse = referanse;
    }


    boolean harGodkjentAktivitet(Inntektsmelding inntektsmelding) {
        var alleYrkesaktiviteter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(behandlingReferanse.getAktørId())).getAlleYrkesaktiviteter();
        var yrkesaktiviteter = alleYrkesaktiviteter.stream().filter(y -> y.gjelderFor(inntektsmelding.getArbeidsgiver(), inntektsmelding.getArbeidsforholdRef())).collect(Collectors.toSet());
        var opptjeningsperioder = yrkesaktiviteter.stream()
            .filter(y -> y.getArbeidType().equals(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD))
            .flatMap(y -> finnOpptjeningsperiodeForYrkesaktivitet(y).stream())
            .toList();
        var aktivitetTilBrukIBeregning = FiltrerOpptjeningaktivitetForBeregning.filtrerForBeregning(vilkårsperiode, opptjeningsperioder, vilkårUtfallMerknad, opptjening);
        return !aktivitetTilBrukIBeregning.isEmpty() || erInaktiv(vilkårUtfallMerknad);

    }

    private static boolean erInaktiv(VilkårUtfallMerknad vilkårUtfallMerknad) {
        return VilkårUtfallMerknad.VM_7847_B.equals(vilkårUtfallMerknad) || VilkårUtfallMerknad.VM_7847_A.equals(vilkårUtfallMerknad);
    }


    private List<OpptjeningsperiodeForSaksbehandling> finnOpptjeningsperiodeForYrkesaktivitet(Yrkesaktivitet y) {
        return mapYrkesaktivitet(behandlingReferanse, y, iayGrunnlag, utfallVurderer, OpptjeningAktivitetType.hentFraArbeidTypeRelasjoner(), opptjening.getOpptjeningPeriode(), vilkårsperiode, tidslinjePrYtelse);
    }

}
