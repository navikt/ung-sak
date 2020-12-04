package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulatorInputTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusRestTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.StartBeregningInput;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.SamletKalkulusResultat;
import no.nav.folketrygdloven.kalkulus.beregning.v1.FrisinnGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedSøkerInfoDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.StartBeregningListeRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.TilstandResponse;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;

/**
 * KalkulusTjeneste sørger for at K9 kaller kalkulus på riktig format i henhold til no.nav.folketrygdloven.kalkulus.kontrakt
 * (https://github.com/navikt/ft-kalkulus/)
 */
@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
public class FrisinnKalkulusTjeneste extends KalkulusTjeneste {

    private InntektArbeidYtelseTjeneste iayTjeneste;

    public FrisinnKalkulusTjeneste() {
    }

    @Inject
    public FrisinnKalkulusTjeneste(KalkulusRestTjeneste restTjeneste,
                                   FagsakRepository fagsakRepository,
                                   @FagsakYtelseTypeRef("FRISINN") KalkulatorInputTjeneste kalkulatorInputTjeneste,
                                   InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                   ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                   VilkårResultatRepository vilkårResultatRepository,
                                   @FagsakYtelseTypeRef("FRISINN") Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper) {
        super(restTjeneste, fagsakRepository, vilkårResultatRepository, kalkulatorInputTjeneste,
            inntektArbeidYtelseTjeneste, arbeidsgiverTjeneste, ytelseGrunnlagMapper);
        this.iayTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public SamletKalkulusResultat fortsettBeregning(BehandlingReferanse referanse, Collection<BgRef> bgReferanser, BehandlingStegType stegType) {
        return super.fortsettBeregning(referanse, bgReferanser, stegType);
    }

    @Override
    public SamletKalkulusResultat startBeregning(BehandlingReferanse ref, List<StartBeregningInput> startBeregningInput) {

        var sortertInput = startBeregningInput.stream().sorted(Comparator.comparing(StartBeregningInput::getSkjæringstidspunkt)).collect(Collectors.toList());
        Map<UUID, KalkulusResultat> uuidKalkulusResulat = new LinkedHashMap<>();
        Map<UUID, KalkulatorInputDto> sendTilKalkulus = new LinkedHashMap<>();
        Collection<BgRef> bgReferanser = startBeregningInput.stream().map(input -> new BgRef(input.getBgReferanse(), input.getSkjæringstidspunkt()))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        var refusjonskravDatoer = iayTjeneste.hentRefusjonskravDatoerForSak(ref.getSaksnummer());
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
        var ytelseGrunnlagMapper = getYtelsesspesifikkMapper(FagsakYtelseType.FRISINN);

        for (var input : sortertInput) {
            var ytelseGrunnlag = ytelseGrunnlagMapper.lagYtelsespesifiktGrunnlag(ref, input.getVilkårsperiode());
            var bgReferanse = input.getBgReferanse();
            var skjæringstidspunkt = input.getSkjæringstidspunkt();
            FrisinnGrunnlag frisinnGrunnlag = (FrisinnGrunnlag) ytelseGrunnlag;
            // frisinn super-hacky håndtering av avslagsårsaker uten for kalkulkus. Slette denne klassen snarest Frisinn er ferdig.
            if (frisinnGrunnlag.getPerioderMedSøkerInfo().isEmpty()) {
                uuidKalkulusResulat.put(bgReferanse, new KalkulusResultat(Collections.emptyList()).medAvslåttVilkår(Avslagsårsak.INGEN_STØNADSDAGER_I_SØKNADSPERIODEN));
            } else {
                // tar en og en
                var startBeregningRequest = initStartRequest(ref, iayGrunnlag, Set.of() /* frisinn har ikke inntektsmeldinger */
                    , refusjonskravDatoer, List.of(new StartBeregningInput(bgReferanse, input.getVilkårsperiode())));

                var inputPerRef = startBeregningRequest.getKalkulatorInputPerKoblingReferanse();
                if (inputPerRef.size() != 1) {
                    throw new IllegalStateException("forventet bare et resultat for startberegning, fikk: " + inputPerRef.size());
                } else {
                    var kalkulatorInput = inputPerRef.values().iterator().next();
                    if (kalkulatorInput.getOpptjeningAktiviteter().getPerioder().isEmpty()) {
                        if (erSøktFrilansISistePeriode(frisinnGrunnlag.getPerioderMedSøkerInfo())) {
                            uuidKalkulusResulat.put(bgReferanse, new KalkulusResultat(Collections.emptyList()).medAvslåttVilkår(Avslagsårsak.SØKT_FRILANS_UTEN_FRILANS_INNTEKT));
                        } else {
                            uuidKalkulusResulat.put(bgReferanse, new KalkulusResultat(Collections.emptyList()).medAvslåttVilkår(Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG));
                        }
                    } else {
                        // hacky, men denne klassen skulle aldri eksistert. Slettes snarest Frisinn er ferdig.
                        sendTilKalkulus.putAll(inputPerRef);
                    }
                }
            }

        }

        if (!sendTilKalkulus.isEmpty()) {
            var fraBeregningResponse = beregnKalkulus(ref, sendTilKalkulus, bgReferanser);
            uuidKalkulusResulat.putAll(fraBeregningResponse.getResultater());
        }

        return new SamletKalkulusResultat(uuidKalkulusResulat, bgReferanser);

    }

    private SamletKalkulusResultat beregnKalkulus(BehandlingReferanse ref, Map<UUID, KalkulatorInputDto> sendTilKalkulus, Collection<BgRef> bgReferanser) {
        // samlet request til beregning
        var startBeregningRequest = new StartBeregningListeRequest(sendTilKalkulus,
            ref.getSaksnummer().getVerdi(),
            new AktørIdPersonident(ref.getAktørId().getId()),
            YtelseTyperKalkulusStøtterKontrakt.FRISINN);
        List<TilstandResponse> tilstandResponse = getKalkulusRestTjeneste().startBeregning(startBeregningRequest);
        var fraBeregningResponse = mapFraTilstand(tilstandResponse, bgReferanser);
        return fraBeregningResponse;
    }

    private boolean erSøktFrilansISistePeriode(List<PeriodeMedSøkerInfoDto> perioderMedSøkerInfo) {
        Optional<PeriodeMedSøkerInfoDto> sistePeriode = perioderMedSøkerInfo.stream().max(Comparator.comparing(o -> o.getPeriode().getTom()));
        return sistePeriode.map(PeriodeMedSøkerInfoDto::getSøkerFrilansIPeriode).orElse(false);
    }

}
