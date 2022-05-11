package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregnForRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregnListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HåndterBeregningListeRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.HåndterBeregningRequest;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;

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

    /** Lager request for beregning
     *
     * @param stegType Stegtype
     * @param referanse Behandlingreferanse
     * @param beregnInput   Liste med referanser og skjæringstidspunkt
     * @param iayGrunnlag   IAY-grunnlag
     * @param sakInntektsmeldinger  Inntektsmeldinger
     * @return Request for beregning
     */
    public BeregnListeRequest lagMedInput(BehandlingStegType stegType,
                                          BehandlingReferanse referanse,
                                          List<BeregnInput> beregnInput,
                                          InntektArbeidYtelseGrunnlag iayGrunnlag,
                                          Collection<Inntektsmelding> sakInntektsmeldinger) {
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(referanse.getFagsakId());
        AktørIdPersonident aktør = new AktørIdPersonident(fagsak.getAktørId().getId());
        return new BeregnListeRequest(
            fagsak.getSaksnummer().getVerdi(),
            referanse.getBehandlingUuid(),
            aktør,
            YtelseTyperKalkulusStøtterKontrakt.fraKode(referanse.getFagsakYtelseType().getKode()),
            new StegType(stegType.getKode()),
            lagRequestForReferanserMedInput(referanse, beregnInput, iayGrunnlag, sakInntektsmeldinger));
    }

    public HåndterBeregningListeRequest lagForAksjonspunktOppdateringMedInput(List<HåndterBeregningRequest> requestListe,
                                                                      BehandlingReferanse referanse,
                                                                      List<BeregnInput> beregnInput,
                                                                      InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                      Collection<Inntektsmelding> sakInntektsmeldinger) {
        var input = lagInputPrReferanse(referanse, iayGrunnlag, sakInntektsmeldinger, beregnInput);
        return new HåndterBeregningListeRequest(requestListe,
            input,
            YtelseTyperKalkulusStøtterKontrakt.fraKode(referanse.getFagsakYtelseType().getKode()),
            referanse.getSaksnummer().getVerdi(),
            referanse.getBehandlingUuid());
    }

    private List<BeregnForRequest> lagRequestForReferanserMedInput(BehandlingReferanse referanse,
                                                                  List<BeregnInput> beregnInput,
                                                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                  Collection<Inntektsmelding> sakInntektsmeldinger) {
        return lagRequestMedKalkulatorInput(referanse, iayGrunnlag, sakInntektsmeldinger, beregnInput);
    }

    private List<BeregnForRequest> lagRequestMedKalkulatorInput(BehandlingReferanse behandlingReferanse,
                                                                InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                Collection<Inntektsmelding> sakInntektsmeldinger,
                                                                List<BeregnInput> beregnInput) {
        var referanseRelasjoner = beregnInput.stream().collect(Collectors.toMap(BeregnInput::getBgReferanse, BeregnInput::getOriginalReferanser));
        Map<UUID, KalkulatorInputDto> input = lagInputPrReferanse(behandlingReferanse, iayGrunnlag, sakInntektsmeldinger, beregnInput);
        return input.entrySet().stream()
            .sorted(Comparator.comparing(e -> e.getValue().getSkjæringstidspunkt()))
            .map(e -> new BeregnForRequest(
                e.getKey(), // KoblingReferanse
                referanseRelasjoner.get(e.getKey()), // Kobling -> Original Referanse relasjon
                e.getValue(), // Kalkulatorinput
                finnForlengelseperiode(e.getKey(), beregnInput) // Forlengelseperioder
            )).toList();
    }

    private List<Periode> finnForlengelseperiode(UUID key, List<BeregnInput> beregnInput) {
        // Foreløpig mappes hele vilkårsperioden som forlengelseperiode. Det betyr at vi tillater endring i hele perioden ved forlengelse.
        return beregnInput.stream().filter(i -> i.getBgReferanse().equals(key))
            .filter(BeregnInput::erForlengelse).findFirst()
            .map(BeregnInput::getVilkårsperiode)
            .map(p -> List.of(new Periode(p.getFomDato(), p.getTomDato())))
            .orElse(null);
    }

    private Map<UUID, KalkulatorInputDto> lagInputPrReferanse(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag iayGrunnlag, Collection<Inntektsmelding> sakInntektsmeldinger, List<BeregnInput> beregnInput) {
        return getInputTjeneste(behandlingReferanse.getFagsakYtelseType()).byggInputPrReferanse(
            behandlingReferanse,
            iayGrunnlag,
            sakInntektsmeldinger,
            beregnInput
        );
    }

    private KalkulatorInputTjeneste getInputTjeneste(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(kalkulatorInputTjeneste, ytelseType).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + KalkulatorInputTjeneste.class.getName() + " mapper for ytelsetype=" + ytelseType));
    }


}
