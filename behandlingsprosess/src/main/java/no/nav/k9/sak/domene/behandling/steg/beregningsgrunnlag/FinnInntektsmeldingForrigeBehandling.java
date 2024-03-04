package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;

class FinnInntektsmeldingForrigeBehandling {

    public static List<Inntektsmelding> finnInntektsmeldingerFraForrigeBehandling(BehandlingReferanse referanse, Collection<Inntektsmelding> inntektsmeldinger, List<MottattDokument> mottatteInntektsmeldinger) {
        return inntektsmeldinger.stream()
            .filter(it -> erInntektsmeldingITidligereBehandling(it, referanse.getBehandlingId(), mottatteInntektsmeldinger))
            .toList();
    }

    private static boolean erInntektsmeldingITidligereBehandling(Inntektsmelding inntektsmelding, Long behandlingId, List<MottattDokument> mottatteInntektsmeldinger) {
        return mottatteInntektsmeldinger.stream()
            .filter(it -> Objects.equals(it.getJournalpostId(), inntektsmelding.getJournalpostId()))
            .noneMatch(md -> Objects.equals(md.getBehandlingId(), behandlingId));
    }

}
