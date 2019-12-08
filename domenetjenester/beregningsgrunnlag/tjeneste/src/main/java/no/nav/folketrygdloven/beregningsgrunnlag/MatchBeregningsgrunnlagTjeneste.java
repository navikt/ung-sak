package no.nav.folketrygdloven.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

/**
 * Tjeneste som finner andeler basert på informasjon om andelen (arbeidsforholdId, andelsnr)
 */
@ApplicationScoped
public class MatchBeregningsgrunnlagTjeneste {

    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;


    MatchBeregningsgrunnlagTjeneste() {
        // for CDI proxy
    }

    @Inject
    public MatchBeregningsgrunnlagTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    /**
     * Finner lik andel i et beregningsgrunnlag, dvs det beregninsgrunnlaget som vises i 95-steget.
     *
     * @param andelINyttBG andel i nytt grunnlag
     * @return Andelen i det gjeldende grunnlaget
     */
    public Optional<BeregningsgrunnlagPrStatusOgAndel> hentLikAndelIBeregningsgrunnlag(BeregningsgrunnlagPrStatusOgAndel andelINyttBG,
                                                                                       BeregningsgrunnlagEntitet beregningsgrunnlag) {
        BeregningsgrunnlagPeriode periodeIGjeldendeBG = finnPeriodeIBeregningsgrunnlag(andelINyttBG.getBeregningsgrunnlagPeriode(),
            beregningsgrunnlag);
        return periodeIGjeldendeBG.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(andelIGjeldendeGrunnlag -> andelIGjeldendeGrunnlag.equals(andelINyttBG))
            .findFirst();
    }


    public static Optional<BeregningsgrunnlagPeriode> finnOverlappendePeriodeOmKunEnFinnes(BeregningsgrunnlagPeriode periode,
                                                                                           Optional<BeregningsgrunnlagEntitet> forrigeGrunnlag) {
        List<BeregningsgrunnlagPeriode> matchedePerioder = forrigeGrunnlag.map(bg ->
            bg.getBeregningsgrunnlagPerioder().stream()
            .filter(periodeIGjeldendeGrunnlag -> periode.getPeriode()
                .overlapper(periodeIGjeldendeGrunnlag.getPeriode())).collect(Collectors.toList())).orElse(Collections.emptyList());
        if (matchedePerioder.size() == 1) {
            return Optional.of(matchedePerioder.get(0));
        }
        return Optional.empty();
    }


    public static BeregningsgrunnlagPeriode finnPeriodeIBeregningsgrunnlag(BeregningsgrunnlagPeriode periode, BeregningsgrunnlagEntitet gjeldendeBeregningsgrunnlag) {

        if (periode.getBeregningsgrunnlagPeriodeFom().isBefore(gjeldendeBeregningsgrunnlag.getSkjæringstidspunkt())) {
            return gjeldendeBeregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
                .min(Comparator.comparing(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPeriodeFom))
                .orElseThrow(() -> new IllegalStateException("Fant ingen perioder i beregningsgrunnlag."));
        }

        return gjeldendeBeregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .filter(bgPeriode -> inkludererBeregningsgrunnlagPeriodeDato(bgPeriode, periode.getBeregningsgrunnlagPeriodeFom()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Finner ingen korresponderende periode i det fastsatte grunnlaget"));
    }

    private static boolean inkludererBeregningsgrunnlagPeriodeDato(BeregningsgrunnlagPeriode periode, LocalDate dato) {
        return !periode.getBeregningsgrunnlagPeriodeFom().isAfter(dato) && (periode.getBeregningsgrunnlagPeriodeTom() == null || !periode.getBeregningsgrunnlagPeriodeTom().isBefore(dato));
    }


    /**
     * Matcher andel fra periode først basert på andelsnr. Om dette gir eit funn returneres andelen. Om dette ikkje
     * gir eit funn matches det på arbeidsforholdId. Om dette ikkje gir eit funn kastes exception.
     *
     * @param periode          beregningsgrunnlagperiode der man leter etter en andel basert på andelsnr og arbeidsforholdId
     * @param andelsnr         andelsnr til andelen det letes etter
     * @param arbeidsforholdId arbeidsforholdId til arbeidsforholdet som andelen er knyttet til
     * @return andel som matcher oppgitt informasjon, ellers kastes exception
     */
    public static BeregningsgrunnlagPrStatusOgAndel matchMedAndelFraPeriode(BeregningsgrunnlagPeriode periode, Long andelsnr, InternArbeidsforholdRef arbeidsforholdId) {
        Optional<BeregningsgrunnlagPrStatusOgAndel> matchetAndel = periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(a -> a.getAndelsnr().equals(andelsnr))
            .findFirst();
        return matchetAndel.orElseGet(() -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(a -> a.getBgAndelArbeidsforhold()
                .map(BGAndelArbeidsforhold::getArbeidsforholdRef)
                .filter(arbeidsforholdRef -> arbeidsforholdRef.gjelderForSpesifiktArbeidsforhold()
                    && arbeidsforholdRef.gjelderFor(arbeidsforholdId))
                .isPresent()
            )
            .findFirst()
            .orElseThrow(() -> MatchBeregningsgrunnlagTjenesteFeil.FACTORY.finnerIkkeAndelFeil().toException()));
    }


    /**
     * Matcher andel fra periode først basert på andelsnr. Om dette gir eit funn returneres andelen. Om dette ikkje gir eit funn kastes exception.
     *
     * @param periode    beregningsgrunnlagperiode der man leter etter en andel basert på andelsnr og arbeidsforholdId
     * @param andelsnr   andelsnr til andelen det letes etter
     * @return andel som matcher oppgitt informasjon, ellers kastes exception
     */
    public static BeregningsgrunnlagPrStatusOgAndel matchMedAndelFraPeriodePåAndelsnr(BeregningsgrunnlagPeriode periode, Long andelsnr) {
        return matchMedAndelFraPeriodePåAndelsnrOmFinnes(periode, andelsnr)
            .orElseThrow(() -> MatchBeregningsgrunnlagTjenesteFeil.FACTORY.finnerIkkeAndelFeil().toException());
    }


    /**
     * Matcher andel fra periode først basert på andelsnr. Om dette gir eit funn returneres andelen.
     *
     * @param periode    beregningsgrunnlagperiode der man leter etter en andel basert på andelsnr og arbeidsforholdId
     * @param andelsnr   andelsnr til andelen det letes etter
     * @return andel som matcher oppgitt informasjon
     */
    private static Optional<BeregningsgrunnlagPrStatusOgAndel> matchMedAndelFraPeriodePåAndelsnrOmFinnes(BeregningsgrunnlagPeriode periode, Long andelsnr) {
        return periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(a -> a.getAndelsnr().equals(andelsnr))
            .findFirst();
    }



    /**
     * Matcher arbeidsforhold i siste beregningsgrunnlag med som ble lagret i steg,
     *
     * @param behandlingId      behandlingId for behandling som har beregningsgrunnlag med tilhørende beregningsgrunnlagperiode
     * @param arbeidsforholdId arbeidsforholdId til arbeidsforholdet som andelen er knyttet til
     * @return andel som matcher oppgitt informasjon, ellers kastes exception
     */
    public Optional<BGAndelArbeidsforhold> matchArbeidsforholdIAktivtGrunnlag(Long behandlingId,
                                                                              String arbeidsgiverId,
                                                                              InternArbeidsforholdRef arbeidsforholdId) {

        return beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingId)
            .map(BeregningsgrunnlagEntitet::getBeregningsgrunnlagPerioder)
            .orElse(Collections.emptyList())
            .stream()
            .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .flatMap(arbeidsforhold -> arbeidsforhold.getBgAndelArbeidsforhold().stream())
            .filter(a -> a.getArbeidsgiver().getIdentifikator().equals(arbeidsgiverId) && a.getArbeidsforholdRef().gjelderFor(arbeidsforholdId))
            .findFirst();
    }

    public interface MatchBeregningsgrunnlagTjenesteFeil extends DeklarerteFeil {

        MatchBeregningsgrunnlagTjeneste.MatchBeregningsgrunnlagTjenesteFeil FACTORY = FeilFactory.create(MatchBeregningsgrunnlagTjeneste.MatchBeregningsgrunnlagTjenesteFeil.class);

        @TekniskFeil(feilkode = "FP-401644", feilmelding = "Finner ikke andelen for eksisterende grunnlag.", logLevel = LogLevel.WARN)
        Feil finnerIkkeAndelFeil();

        @TekniskFeil(feilkode = "FP-401692", feilmelding = "Fant flere enn 1 matchende periode i gjeldende grunnlag.", logLevel = LogLevel.WARN)
        Feil fantFlereEnn1Periode();
    }

}
