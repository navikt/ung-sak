package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregnForRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregnListeRequest;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
public class LagBeregnRequestTjeneste {

    private Instance<KalkulatorInputTjeneste> kalkulatorInputTjeneste;
    private FagsakRepository fagsakRepository;

    @Inject
    public LagBeregnRequestTjeneste(@Any Instance<KalkulatorInputTjeneste> kalkulatorInputTjeneste,
                                    FagsakRepository fagsakRepository) {
        this.kalkulatorInputTjeneste = kalkulatorInputTjeneste;
        this.fagsakRepository = fagsakRepository;
    }

    public LagBeregnRequestTjeneste() {
    }

    public BeregnListeRequest lagMedInput(BehandlingStegType stegType,
                                          BehandlingReferanse referanse,
                                          List<BeregnInput> beregnInput,
                                          InntektArbeidYtelseGrunnlag iayGrunnlag,
                                          Collection<Inntektsmelding> sakInntektsmeldinger) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(referanse.getFagsakId());
        AktørIdPersonident aktør = new AktørIdPersonident(fagsak.getAktørId().getId());
        return new BeregnListeRequest(
            fagsak.getSaksnummer().getVerdi(),
            aktør,
            YtelseTyperKalkulusStøtterKontrakt.fraKode(referanse.getFagsakYtelseType().getKode()),
            new StegType(stegType.getKode()),
            lagRequestForReferanserMedInput(referanse, beregnInput, iayGrunnlag, sakInntektsmeldinger));
    }

    public List<BeregnForRequest> lagRequestForReferanserMedInput(BehandlingReferanse referanse,
                                                                  List<BeregnInput> beregnInput,
                                                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                  Collection<Inntektsmelding> sakInntektsmeldinger) {
        return lagRequestMedKalkulatorInput(referanse, iayGrunnlag, sakInntektsmeldinger, beregnInput, Collections.emptyList());
    }

    public List<BeregnForRequest> lagRequestForReferanserForForlengelse(BehandlingReferanse referanse,
                                                                        List<BeregnInput> beregnInput,
                                                                        InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                        Collection<Inntektsmelding> sakInntektsmeldinger,
                                                                        List<DatoIntervallEntitet> forlengelseperioder) {
        return lagRequestMedKalkulatorInput(referanse, iayGrunnlag, sakInntektsmeldinger, beregnInput, forlengelseperioder);
    }


    private List<BeregnForRequest> lagRequestMedKalkulatorInput(BehandlingReferanse behandlingReferanse,
                                                                InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                Collection<Inntektsmelding> sakInntektsmeldinger,
                                                                List<BeregnInput> beregnInput, List<DatoIntervallEntitet> forlengelseperioder) {
        var referanseRelasjoner = beregnInput.stream().collect(Collectors.toMap(BeregnInput::getBgReferanse, BeregnInput::getOriginalReferanser));
        Map<UUID, KalkulatorInputDto> input = getInputTjeneste(behandlingReferanse.getFagsakYtelseType()).byggInputPrReferanse(
            behandlingReferanse,
            iayGrunnlag,
            sakInntektsmeldinger,
            beregnInput.stream().collect(Collectors.toMap(BeregnInput::getBgReferanse,BeregnInput::getSkjæringstidspunkt))
        );
        return input.entrySet().stream()
            .map(e -> new BeregnForRequest(
                e.getKey(), // KoblingReferanse
                referanseRelasjoner.get(e.getKey()), // Kobling -> Original Referanse relasjon
                e.getValue(), // Kalkulatorinput
                forlengelseperioder.stream().map(p -> new Periode(p.getFomDato(), p.getTomDato())).toList() // Forlengelseperioder
            )).toList();
    }

    public KalkulatorInputTjeneste getInputTjeneste(FagsakYtelseType ytelseType) {
        var ytelseTypeKode = ytelseType.getKode();
        return FagsakYtelseTypeRef.Lookup.find(kalkulatorInputTjeneste, ytelseTypeKode).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + KalkulatorInputTjeneste.class.getName() + " mapper for ytelsetype=" + ytelseTypeKode));
    }


}
