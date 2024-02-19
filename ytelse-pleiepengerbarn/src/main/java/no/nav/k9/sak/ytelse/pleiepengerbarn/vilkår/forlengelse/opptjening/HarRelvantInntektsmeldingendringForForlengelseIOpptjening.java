package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.forlengelse.opptjening;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.HarEndretInntektsmeldingVurderer;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.typer.JournalpostId;

@ApplicationScoped
public class HarRelvantInntektsmeldingendringForForlengelseIOpptjening implements HarEndretInntektsmeldingVurderer.InntektsmeldingerEndringsvurderer {


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
    public boolean erEndret(Collection<Inntektsmelding> relevanteInntektsmeldinger, Collection<Inntektsmelding> relevanteInntektsmeldingerForrigeVedtak) {
        return harFlereInntektsmeldingerEnnForrigeVedtak(relevanteInntektsmeldingerForrigeVedtak.stream()
            .map(Inntektsmelding::getJournalpostId)
            .collect(Collectors.toSet()), relevanteInntektsmeldinger.stream()
            .map(Inntektsmelding::getJournalpostId)
            .collect(Collectors.toSet()));

    }
    static boolean harFlereInntektsmeldingerEnnForrigeVedtak(Set<JournalpostId> forrigeVedtakJournalposter, Set<JournalpostId> denneBehandlingJournalposter) {
        var nyeJournalposter = denneBehandlingJournalposter.stream().filter(id -> !forrigeVedtakJournalposter.contains(id)).collect(Collectors.toSet());
        return !nyeJournalposter.isEmpty();
    }

}
