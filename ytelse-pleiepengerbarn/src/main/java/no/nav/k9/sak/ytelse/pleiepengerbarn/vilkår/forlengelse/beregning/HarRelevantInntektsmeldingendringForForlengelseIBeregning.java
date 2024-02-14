package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

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
import no.nav.k9.sak.domene.iay.modell.NaturalYtelse;

@ApplicationScoped
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@VilkårTypeRef(VilkårType.OPPTJENINGSVILKÅRET)
public class HarRelevantInntektsmeldingendringForForlengelseIBeregning implements InntektsmeldingerEndringsvurderer {

    @Override
    public Collection<Inntektsmelding> finnInntektsmeldingerMedRelevanteEndringer(Collection<Inntektsmelding> relevanteInntektsmeldinger, Collection<Inntektsmelding> relevanteInntektsmeldingerForrigeVedtak) {

        var nyeInntektsmeldinger = finnNyeInntektsmeldinger(relevanteInntektsmeldinger, relevanteInntektsmeldingerForrigeVedtak);

        return nyeInntektsmeldinger.stream().filter(im -> harEndretBeløpFraForrige(relevanteInntektsmeldingerForrigeVedtak, im) ||
                harEndretNaturalytelserFraForrige(relevanteInntektsmeldingerForrigeVedtak, im)).collect(Collectors.toSet());
    }


    private static Set<Inntektsmelding> finnNyeInntektsmeldinger(Collection<Inntektsmelding> relevanteInntektsmeldinger, Collection<Inntektsmelding> relevanteInntektsmeldingerForrigeVedtak) {
        return relevanteInntektsmeldinger.stream().filter(im -> relevanteInntektsmeldingerForrigeVedtak.stream().noneMatch(it -> it.getJournalpostId().equals(im.getJournalpostId()))).collect(Collectors.toSet());
    }


    private static boolean harEndretNaturalytelserFraForrige(Collection<Inntektsmelding> relevanteInntektsmeldingerForrigeVedtak, Inntektsmelding im) {
        var matchendeIM = relevanteInntektsmeldingerForrigeVedtak.stream().filter(imForrige -> imForrige.getArbeidsgiver().equals(im.getArbeidsgiver()) && imForrige.getArbeidsforholdRef().equals(im.getArbeidsforholdRef()))
            .toList();

        if (matchendeIM.size() != 1) {
            return true;
        }

        var matchFraForrige = matchendeIM.get(0);
        var harLikeMangeNaturalytelser = erLikeStore(matchFraForrige.getNaturalYtelser(), im.getNaturalYtelser());
        return !harLikeMangeNaturalytelser || im.getNaturalYtelser().stream().anyMatch(naturalYtelse -> harIngenMatchendeNaturalytelse(naturalYtelse, matchFraForrige));
    }

    private static boolean harIngenMatchendeNaturalytelse(NaturalYtelse naturalYtelse, Inntektsmelding inntektmelding) {
        return inntektmelding.getNaturalYtelser().stream().noneMatch(
            naturalYtelseForrige -> erLike(naturalYtelse, naturalYtelseForrige)
        );
    }

    private static boolean erLike(NaturalYtelse naturalYtelse, NaturalYtelse naturalYtelseForrige) {
        return naturalYtelseForrige.getType().equals(naturalYtelse.getType()) &&
            naturalYtelseForrige.getPeriode().equals(naturalYtelse.getPeriode()) &&
            naturalYtelseForrige.getBeloepPerMnd().compareTo(naturalYtelse.getBeloepPerMnd()) == 0;
    }

    private static boolean harEndretBeløpFraForrige(Collection<Inntektsmelding> relevanteInntektsmeldingerForrigeVedtak, Inntektsmelding im) {
        var matchendeIM = relevanteInntektsmeldingerForrigeVedtak.stream().filter(imForrige -> imForrige.getArbeidsgiver().equals(im.getArbeidsgiver()) && imForrige.getArbeidsforholdRef().equals(im.getArbeidsforholdRef()))
            .toList();

        if (matchendeIM.size() != 1) {
            return true;
        }

        var matchFraForrige = matchendeIM.get(0);
        return matchFraForrige.getInntektBeløp().compareTo(im.getInntektBeløp()) != 0;
    }

    private static <V> boolean erLikeStore(Collection<V> c1, Collection<V> c2) {
        return c1.size() == c2.size();
    }
}
