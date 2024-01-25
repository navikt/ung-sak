package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Sammenligner sett av inntektsmeldinger mellom forrige og gjeldene behandling
 */
@ApplicationScoped
public class HarEndretInntektsmeldingVurderer {

    private BehandlingRepository behandlingRepository;
    private Instance<InntektsmeldingRelevantForBeregningVilkårsvurdering> inntektsmeldingRelevantForBeregningVilkårsvurdering;


    public HarEndretInntektsmeldingVurderer() {
    }

    @Inject
    public HarEndretInntektsmeldingVurderer(BehandlingRepository behandlingRepository,
                                            @Any Instance<InntektsmeldingRelevantForBeregningVilkårsvurdering> inntektsmeldingRelevantForBeregningVilkårsvurdering) {
        this.behandlingRepository = behandlingRepository;

        this.inntektsmeldingRelevantForBeregningVilkårsvurdering = inntektsmeldingRelevantForBeregningVilkårsvurdering;
    }


    public boolean harEndringPåInntektsmeldingerTilBrukForPerioden(BehandlingReferanse referanse,
                                                                   Collection<Inntektsmelding> inntektsmeldinger,
                                                                   List<MottattDokument> mottatteInntektsmeldinger,
                                                                   DatoIntervallEntitet periode,
                                                                   InntektsmeldingerEndringsvurderer endringsvurderer) {


        var originalBehandling = behandlingRepository.hentBehandling(referanse.getOriginalBehandlingId().orElseThrow());

        var inntektsmeldingerForrigeVedtak = finnInntektsmeldingerFraForrigeVedtak(referanse, inntektsmeldinger, mottatteInntektsmeldinger);


        var inntektsmeldingFiltreringTjeneste = InntektsmeldingRelevantForBeregningVilkårsvurdering.finnTjeneste(inntektsmeldingRelevantForBeregningVilkårsvurdering, referanse.getFagsakYtelseType());
        var relevanteInntektsmeldingerForrigeVedtak = inntektsmeldingFiltreringTjeneste.begrensInntektsmeldinger(BehandlingReferanse.fra(originalBehandling), inntektsmeldingerForrigeVedtak, periode);
        var relevanteInntektsmeldinger = inntektsmeldingFiltreringTjeneste.begrensInntektsmeldinger(referanse, inntektsmeldinger, periode);

        return endringsvurderer.erEndret(relevanteInntektsmeldinger, relevanteInntektsmeldingerForrigeVedtak);
    }

    private List<Inntektsmelding> finnInntektsmeldingerFraForrigeVedtak(BehandlingReferanse referanse, Collection<Inntektsmelding> inntektsmeldinger, List<MottattDokument> mottatteInntektsmeldinger) {
        return inntektsmeldinger.stream()
            .filter(it -> erInntektsmeldingITidligereBehandling(it, referanse.getBehandlingId(), mottatteInntektsmeldinger))
            .toList();
    }


    private boolean erInntektsmeldingITidligereBehandling(Inntektsmelding inntektsmelding, Long behandlingId, List<MottattDokument> mottatteInntektsmeldinger) {
        return mottatteInntektsmeldinger.stream()
            .filter(it -> Objects.equals(it.getJournalpostId(), inntektsmelding.getJournalpostId()))
            .anyMatch(md -> !Objects.equals(md.getBehandlingId(), behandlingId));
    }

    @FunctionalInterface
    public interface InntektsmeldingerEndringsvurderer {

        boolean erEndret(Collection<Inntektsmelding> gjeldendeInntektsmeldinger, Collection<Inntektsmelding> inntektsmeldingerForrigeVedtak);

    }


}
