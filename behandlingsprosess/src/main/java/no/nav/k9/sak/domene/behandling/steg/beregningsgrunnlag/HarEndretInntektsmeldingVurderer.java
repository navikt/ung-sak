package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.FinnInntektsmeldingForrigeBehandling.finnInntektsmeldingerFraForrigeBehandling;

import java.util.Collection;
import java.util.List;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

/**
 * Sammenligner sett av inntektsmeldinger mellom forrige og gjeldene behandling
 */
public class HarEndretInntektsmeldingVurderer {

    private final InntektsmeldingFilter filter;

    private final InntektsmeldingerEndringsvurderer endringsvurderer;

    public HarEndretInntektsmeldingVurderer(InntektsmeldingFilter filter,
                                            InntektsmeldingerEndringsvurderer endringsvurderer) {
        this.filter = filter;
        this.endringsvurderer = endringsvurderer;
    }


    public boolean harEndringPåInntektsmeldingerTilBrukForPerioden(BehandlingReferanse referanse,
                                                                   BehandlingReferanse referanseOriginalBehandling,
                                                                   DatoIntervallEntitet periode,
                                                                   Collection<Inntektsmelding> inntektsmeldinger,
                                                                   List<MottattDokument> mottatteInntektsmeldinger) {
        var inntektsmeldingerForrigeVedtak = finnInntektsmeldingerFraForrigeBehandling(referanse, inntektsmeldinger, mottatteInntektsmeldinger);
        var relevanteInntektsmeldingerForrigeVedtak = filter.filtrer(referanseOriginalBehandling, inntektsmeldingerForrigeVedtak, periode);
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
