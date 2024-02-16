package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.opptjening;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.InntektsmeldingerEndringsvurderer;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@VilkårTypeRef(VilkårType.OPPTJENINGSVILKÅRET)
public class HarRelevantInntektsmeldingendringForForlengelseIOpptjening implements InntektsmeldingerEndringsvurderer {


    /**
     * Finner ut om det er relevante endringer siden forrige behandling.
     * <p>
     * Det er kun tilkomne inntektsmeldinger som regnes som relevante endringer for opptjening. Bortfalte gir ikke grunnlag for revurdering. En inntektsmelding kan "bortfalle" dersom opptjening vurderes avslått eller det er registerendringer tilbake i tid.
     *
     * @param relevanteInntektsmeldinger              Inntektsmeldinger i behandlingen
     * @param relevanteInntektsmeldingerForrigeVedtak Inntektsmeldinger fra forrige vedtak
     * @return Verdi som sier om vi har relevante endringer
     */
    @Override
    public Collection<Inntektsmelding> finnInntektsmeldingerMedRelevanteEndringer(Collection<Inntektsmelding> relevanteInntektsmeldinger, Collection<Inntektsmelding> relevanteInntektsmeldingerForrigeVedtak) {
        return finnNyeInntektsmeldinger(relevanteInntektsmeldinger, relevanteInntektsmeldingerForrigeVedtak);

    }

    private static Set<Inntektsmelding> finnNyeInntektsmeldinger(Collection<Inntektsmelding> relevanteInntektsmeldinger, Collection<Inntektsmelding> relevanteInntektsmeldingerForrigeVedtak) {
        return relevanteInntektsmeldinger.stream().filter(im -> relevanteInntektsmeldingerForrigeVedtak.stream()
            .noneMatch(it -> it.gjelderSammeArbeidsforhold(im))).collect(Collectors.toSet());
    }
}
