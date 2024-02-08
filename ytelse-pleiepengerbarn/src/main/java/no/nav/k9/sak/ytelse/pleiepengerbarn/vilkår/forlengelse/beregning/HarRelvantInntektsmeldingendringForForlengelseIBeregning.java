package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import java.util.Collection;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.HarEndretInntektsmeldingVurderer;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.typer.JournalpostId;

@ApplicationScoped
public class HarRelvantInntektsmeldingendringForForlengelseIBeregning implements HarEndretInntektsmeldingVurderer.InntektsmeldingerEndringsvurderer {

    @Override
    public boolean erEndret(Collection<Inntektsmelding> relevanteInntektsmeldinger, Collection<Inntektsmelding> relevanteInntektsmeldingerForrigeVedtak) {
        return !erLikeStore(relevanteInntektsmeldinger, relevanteInntektsmeldingerForrigeVedtak) || relevanteInntektsmeldinger.stream().anyMatch(im -> harEndretBeløpFraForrige(relevanteInntektsmeldingerForrigeVedtak, im));
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

    static boolean harUlikeJournalposter(Set<JournalpostId> forrigeVedtakJournalposter, Set<JournalpostId> denneBehandlingJournalposter) {

        var erLikeStore = erLikeStore(forrigeVedtakJournalposter, denneBehandlingJournalposter);

        var inneholderDeSamme = denneBehandlingJournalposter.containsAll(forrigeVedtakJournalposter);

        return !(erLikeStore && inneholderDeSamme);
    }

    private static <V> boolean erLikeStore(Collection<V> c1, Collection<V> c2) {
        return c1.size() == c2.size();
    }
}
