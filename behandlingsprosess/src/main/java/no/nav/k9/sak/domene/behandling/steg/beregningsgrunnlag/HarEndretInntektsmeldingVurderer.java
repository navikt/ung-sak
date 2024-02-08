package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.FinnInntektsmeldingForrigeBehandling.finnInntektsmeldingerFraForrigeBehandling;

import java.util.Collection;
import java.util.List;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Sammenligner sett av inntektsmeldinger mellom forrige og gjeldene behandling
 */
public class HarEndretInntektsmeldingVurderer {

    private final BehandlingRepository behandlingRepository;
    private final InntektsmeldingFilter filter;

    private final InntektsmeldingerEndringsvurderer endringsvurderer;

    public HarEndretInntektsmeldingVurderer(BehandlingRepository behandlingRepository,
                                            InntektsmeldingFilter filter,
                                            InntektsmeldingerEndringsvurderer endringsvurderer) {
        this.behandlingRepository = behandlingRepository;
        this.filter = filter;
        this.endringsvurderer = endringsvurderer;
    }


    public boolean harEndringPåInntektsmeldingerTilBrukForPerioden(BehandlingReferanse referanse,
                                                                   DatoIntervallEntitet periode,
                                                                   Collection<Inntektsmelding> inntektsmeldinger,
                                                                   List<MottattDokument> mottatteInntektsmeldinger) {


        if (!referanse.erRevurdering()) {
            throw new IllegalArgumentException("Endringsutleder for inntektsmelding skal kun kjøres i kontekst av en revurdering");
        }
        var originalBehandling = behandlingRepository.hentBehandling(referanse.getOriginalBehandlingId().orElseThrow());

        var inntektsmeldingerForrigeVedtak = finnInntektsmeldingerFraForrigeBehandling(referanse, inntektsmeldinger, mottatteInntektsmeldinger);
        var relevanteInntektsmeldingerForrigeVedtak = filter.filtrer(BehandlingReferanse.fra(originalBehandling), inntektsmeldingerForrigeVedtak, periode);
        var relevanteInntektsmeldinger = filter.filtrer(referanse, inntektsmeldinger, periode);
        return endringsvurderer.erEndret(relevanteInntektsmeldinger, relevanteInntektsmeldingerForrigeVedtak);
    }



    @FunctionalInterface
    public interface InntektsmeldingerEndringsvurderer {

        boolean erEndret(Collection<Inntektsmelding> gjeldendeInntektsmeldinger, Collection<Inntektsmelding> inntektsmeldingerForrigeVedtak);

    }



    @FunctionalInterface
    public interface InntektsmeldingFilter {

        Collection<Inntektsmelding> filtrer(BehandlingReferanse referanse, Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode);

    }

}
