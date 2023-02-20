package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.FRISINN;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregnInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.FinnInntektsmeldingForBeregning;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusRestKlient;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.LagBeregnRequestTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.SamletKalkulusResultat;
import no.nav.folketrygdloven.kalkulus.beregning.v1.FrisinnGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedSøkerInfoDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregnForRequest;
import no.nav.folketrygdloven.kalkulus.request.v1.BeregnListeRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;

/**
 * KalkulusTjeneste sørger for at K9 kaller kalkulus på riktig format i henhold til no.nav.folketrygdloven.kalkulus.kontrakt
 * (https://github.com/navikt/ft-kalkulus/)
 */
@ApplicationScoped
@FagsakYtelseTypeRef(FRISINN)
public class FrisinnKalkulusTjeneste extends KalkulusTjeneste {


    public FrisinnKalkulusTjeneste() {
    }

    @Inject
    public FrisinnKalkulusTjeneste(KalkulusRestKlient restTjeneste,
                                   InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                   VilkårResultatRepository vilkårResultatRepository,
                                   LagBeregnRequestTjeneste beregnRequestTjeneste,
                                   @FagsakYtelseTypeRef(FRISINN) Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper,
                                   FinnInntektsmeldingForBeregning finnInntektsmeldingForBeregning) {
        super(restTjeneste, vilkårResultatRepository,
            inntektArbeidYtelseTjeneste, ytelseGrunnlagMapper, beregnRequestTjeneste, finnInntektsmeldingForBeregning);
    }

    @Override
    public SamletKalkulusResultat beregn(BehandlingReferanse ref, List<BeregnInput> beregnInput, BehandlingStegType stegType) {

        var sortertInput = beregnInput.stream().sorted(Comparator.comparing(BeregnInput::getSkjæringstidspunkt)).collect(Collectors.toList());
        Map<UUID, KalkulusResultat> uuidKalkulusResulat = new LinkedHashMap<>();
        List<BeregnForRequest> sendTilKalkulus = new LinkedList<>();
        Collection<BgRef> bgReferanser = beregnInput.stream().map(input -> new BgRef(input.getBgReferanse(), input.getSkjæringstidspunkt()))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        var ytelseGrunnlagMapper = getYtelsesspesifikkMapper(FRISINN);

        for (var input : sortertInput) {
            var ytelseGrunnlag = ytelseGrunnlagMapper.lagYtelsespesifiktGrunnlag(ref, input.getVilkårsperiode());
            var bgReferanse = input.getBgReferanse();
            FrisinnGrunnlag frisinnGrunnlag = (FrisinnGrunnlag) ytelseGrunnlag;
            // frisinn super-hacky håndtering av avslagsårsaker uten for kalkulkus. Slette denne klassen snarest Frisinn er ferdig.
            if (frisinnGrunnlag.getPerioderMedSøkerInfo().isEmpty()) {
                uuidKalkulusResulat.put(bgReferanse, new KalkulusResultat(Collections.emptyList()).medAvslåttVilkår(Avslagsårsak.INGEN_STØNADSDAGER_I_SØKNADSPERIODEN));
            } else {
                // tar en og en
                var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
                var startBeregningRequest = beregnRequestTjeneste.lagMedInput(stegType, ref, beregnInput, iayGrunnlag, Collections.emptyList());
                var inputPerRef = startBeregningRequest.getBeregnForListe();
                if (inputPerRef.size() != 1) {
                    throw new IllegalStateException("forventet bare et resultat for startberegning, fikk: " + inputPerRef.size());
                } else {
                    var kalkulatorInput = inputPerRef.iterator().next().getKalkulatorInput();
                    if (kalkulatorInput.getOpptjeningAktiviteter().getPerioder().isEmpty()) {
                        if (erSøktFrilansISistePeriode(frisinnGrunnlag.getPerioderMedSøkerInfo())) {
                            uuidKalkulusResulat.put(bgReferanse, new KalkulusResultat(Collections.emptyList()).medAvslåttVilkår(Avslagsårsak.SØKT_FRILANS_UTEN_FRILANS_INNTEKT));
                        } else {
                            uuidKalkulusResulat.put(bgReferanse, new KalkulusResultat(Collections.emptyList()).medAvslåttVilkår(Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG));
                        }
                    } else {
                        // hacky, men denne klassen skulle aldri eksistert. Slettes snarest Frisinn er ferdig.
                        sendTilKalkulus.addAll(inputPerRef);
                    }
                }
            }

        }

        if (!sendTilKalkulus.isEmpty()) {
            var fraBeregningResponse = beregnKalkulus(ref, sendTilKalkulus, bgReferanser, stegType);
            uuidKalkulusResulat.putAll(fraBeregningResponse.getResultater());
        }

        return new SamletKalkulusResultat(uuidKalkulusResulat, bgReferanser);

    }

    private SamletKalkulusResultat beregnKalkulus(BehandlingReferanse ref, List<BeregnForRequest> sendTilKalkulus, Collection<BgRef> bgReferanser, BehandlingStegType stegType) {
        // samlet request til beregning
        var startBeregningRequest = new BeregnListeRequest(
            ref.getSaksnummer().getVerdi(),
            ref.getBehandlingUuid(),
            new AktørIdPersonident(ref.getAktørId().getId()),
            YtelseTyperKalkulusStøtterKontrakt.FRISINN,
            new StegType(stegType.getKode()),
            sendTilKalkulus);
        List<TilstandResponse> tilstandResponse = getKalkulusRestTjeneste().beregn(startBeregningRequest).getTilstand();
        var fraBeregningResponse = mapFraTilstand(tilstandResponse, bgReferanser);
        return fraBeregningResponse;
    }

    private boolean erSøktFrilansISistePeriode(List<PeriodeMedSøkerInfoDto> perioderMedSøkerInfo) {
        Optional<PeriodeMedSøkerInfoDto> sistePeriode = perioderMedSøkerInfo.stream().max(Comparator.comparing(o -> o.getPeriode().getTom()));
        return sistePeriode.map(PeriodeMedSøkerInfoDto::getSøkerFrilansIPeriode).orElse(false);
    }

}
