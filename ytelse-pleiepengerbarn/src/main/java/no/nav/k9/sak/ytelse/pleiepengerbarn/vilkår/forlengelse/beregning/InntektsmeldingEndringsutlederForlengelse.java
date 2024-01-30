package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.beregning;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.HarEndretInntektsmeldingVurderer;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.typer.JournalpostId;

@ApplicationScoped
public class InntektsmeldingEndringsutlederForlengelse implements HarEndretInntektsmeldingVurderer.InntektsmeldingerEndringsvurderer {

    private boolean skalVurdereEndretInntekt;

    public InntektsmeldingEndringsutlederForlengelse() {
    }

    @Inject
    public InntektsmeldingEndringsutlederForlengelse(@KonfigVerdi(value = "IM_SKAL_VURDERE_ENDRET_INNTEKT", defaultVerdi = "false") boolean skalVurdereEndretInntekt) {
        this.skalVurdereEndretInntekt = skalVurdereEndretInntekt;
    }

    @Override
    public boolean erEndret(Collection<Inntektsmelding> relevanteInntektsmeldinger, Collection<Inntektsmelding> relevanteInntektsmeldingerForrigeVedtak) {
        var erJournalposterUlike = harUlikeJournalposter(relevanteInntektsmeldingerForrigeVedtak.stream()
            .map(Inntektsmelding::getJournalpostId)
            .collect(Collectors.toSet()), relevanteInntektsmeldinger.stream()
            .map(Inntektsmelding::getJournalpostId)
            .collect(Collectors.toSet()));

        if (!erJournalposterUlike) {
            return false;
        }

        if (!skalVurdereEndretInntekt) {
            return true;
        }

        return relevanteInntektsmeldinger.stream().anyMatch(im -> harEndretBeløpFraForrige(relevanteInntektsmeldingerForrigeVedtak, im));

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

        var erLikeStore = forrigeVedtakJournalposter.size() == denneBehandlingJournalposter.size();

        var inneholderDeSamme = denneBehandlingJournalposter.containsAll(forrigeVedtakJournalposter);

        return !(erLikeStore && inneholderDeSamme);
    }
}
