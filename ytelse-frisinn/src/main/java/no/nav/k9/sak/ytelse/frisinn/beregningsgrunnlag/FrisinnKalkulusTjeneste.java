package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulatorInputTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusRestTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.StartBeregningInput;
import no.nav.folketrygdloven.beregningsgrunnlag.output.KalkulusResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.SamletKalkulusResultat;
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
import no.nav.k9.sak.typer.Saksnummer;

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
                                   VilkårResultatRepository vilkårResultatRepository) {
        super(restTjeneste, fagsakRepository, vilkårResultatRepository, kalkulatorInputTjeneste, inntektArbeidYtelseTjeneste, arbeidsgiverTjeneste);
        this.iayTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public SamletKalkulusResultat fortsettBeregning(FagsakYtelseType fagsakYtelseType, Saksnummer saksnummer, Map<UUID, LocalDate> bgReferanser, BehandlingStegType stegType) {
        return super.fortsettBeregning(fagsakYtelseType, saksnummer, bgReferanser, stegType);
    }

    @Override
    public SamletKalkulusResultat startBeregning(BehandlingReferanse ref, List<StartBeregningInput> startBeregningInput) {

        var sortertInput = startBeregningInput.stream().sorted(Comparator.comparing(StartBeregningInput::getSkjæringstidspunkt)).collect(Collectors.toList());
        Map<UUID, KalkulusResultat> uuidKalkulusResulat = new LinkedHashMap<>();
        Map<UUID, KalkulatorInputDto> sendTilKalkulus = new LinkedHashMap<>();
        Map<UUID, LocalDate> uuidTilStp = new LinkedHashMap<>();
        startBeregningInput.stream().forEach(input -> uuidTilStp.put(input.getBgReferanse(), input.getSkjæringstidspunkt()));

        var refusjonskravDatoer = iayTjeneste.hentRefusjonskravDatoerForSak(ref.getSaksnummer());
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
        var sakInntektsmeldinger = iayTjeneste.hentInntektsmeldinger(ref.getSaksnummer());

        for (var input : sortertInput) {
            var ytelseGrunnlag = input.getYtelseGrunnlag();
            var bgReferanse = input.getBgReferanse();
            var skjæringstidspunkt = input.getSkjæringstidspunkt();
            FrisinnGrunnlag frisinnGrunnlag = (FrisinnGrunnlag) ytelseGrunnlag;
            // frisinn super-hacky håndtering av avslagsårsaker uten for kalkulkus. Slette denne klassen snarest Frisinn er ferdig.
            if (frisinnGrunnlag.getPerioderMedSøkerInfo().isEmpty()) {
                uuidKalkulusResulat.put(bgReferanse, new KalkulusResultat(Collections.emptyList()).medAvslåttVilkår(Avslagsårsak.INGEN_STØNADSDAGER_I_SØKNADSPERIODEN));
            } else {
                // tar en og en
                var startBeregningRequest = initStartRequest(ref, iayGrunnlag, sakInntektsmeldinger, refusjonskravDatoer,
                    List.of(new StartBeregningInput(bgReferanse, skjæringstidspunkt, ytelseGrunnlag)));

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

        var fraBeregningResponse = beregnKalkulus(ref, sendTilKalkulus, uuidTilStp);
        uuidKalkulusResulat.putAll(fraBeregningResponse.getResultater());

        return new SamletKalkulusResultat(uuidKalkulusResulat, uuidTilStp);

    }

    private SamletKalkulusResultat beregnKalkulus(BehandlingReferanse ref, Map<UUID, KalkulatorInputDto> sendTilKalkulus, Map<UUID, LocalDate> uuidTilStp) {
        // samlet request til beregning
        var startBeregningRequest = new StartBeregningListeRequest(sendTilKalkulus,
            ref.getSaksnummer().getVerdi(),
            new AktørIdPersonident(ref.getAktørId().getId()),
            YtelseTyperKalkulusStøtterKontrakt.FRISINN);
        List<TilstandResponse> tilstandResponse = getKalkulusRestTjeneste().startBeregning(startBeregningRequest);
        var fraBeregningResponse = mapFraTilstand(tilstandResponse, uuidTilStp);
        return fraBeregningResponse;
    }

    private boolean erSøktFrilansISistePeriode(List<PeriodeMedSøkerInfoDto> perioderMedSøkerInfo) {
        Optional<PeriodeMedSøkerInfoDto> sistePeriode = perioderMedSøkerInfo.stream().max(Comparator.comparing(o -> o.getPeriode().getTom()));
        return sistePeriode.map(PeriodeMedSøkerInfoDto::getSøkerFrilansIPeriode).orElse(false);
    }

}
