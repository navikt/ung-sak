package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import static no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.BortfaltOgTilkommetArbeidsgiver.utledAntallArbeidsgivereTidslinje;
import static no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.BortfaltOgTilkommetArbeidsgiver.utledTilkomneArbeidsgivereTidslinje;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.ArbeidPeriode;

public class PerioderMedInaktivitetUtleder {


    public Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> utled(InaktivitetUtlederInput input) {
        var tidslinjeTilVurdering = input.getTidslinjeTilVurdering();

        if (tidslinjeTilVurdering.isEmpty()) {
            return Map.of();
        }

        var ikkeAktivTidslinje = new LocalDateTimeline<>(tidslinjeTilVurdering.toSegments()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getLocalDateInterval(), true))
            .collect(Collectors.toList()));

        var aktørArbeid = input.getIayGrunnlag().getAktørArbeidFraRegister(input.getBrukerAktørId());

        var mellomregning = new HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>>();

        var alleYrkesaktiviteter = aktørArbeid.map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList());

        LocalDateTimeline<BigDecimal> antallArbeidsgivereTidslinje = input.skalKjøreNyLogikkForSpeiling() ? utledAntallArbeidsgivereTidslinje(alleYrkesaktiviteter) : LocalDateTimeline.empty();
        alleYrkesaktiviteter
            .stream()
            .filter(it -> ArbeidType.AA_REGISTER_TYPER.contains(it.getArbeidType()))
            .forEach(yrkesaktivitet -> mapYrkesAktivitet(mellomregning, yrkesaktivitet));

        var arbeidsgivereVedStartPrSkjæringstidspunkt = finnAktiveArbeidsgiverePrSkjæringstidspunktFraBeregning(input);
        return utledBortfallendeAktiviteterSomSkalFortsattKompenseres(mellomregning, antallArbeidsgivereTidslinje,
            ikkeAktivTidslinje, arbeidsgivereVedStartPrSkjæringstidspunkt, input.skalDefinereIkkeYrkesaktivFraBg());
    }

    private static Map<LocalDate, Set<Arbeidsgiver>> finnAktiveArbeidsgiverePrSkjæringstidspunktFraBeregning(InaktivitetUtlederInput input) {
        return input.getBeregningsgrunnlag().stream()
            .filter(bg -> !bg.getBeregningsgrunnlagPerioder().isEmpty())
            .collect(
                Collectors.toMap(
                    Beregningsgrunnlag::getSkjæringstidspunkt,
                    bg -> bg.getBeregningsgrunnlagPerioder().get(0)
                        .getBeregningsgrunnlagPrStatusOgAndelList()
                        .stream()
                        .map(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(BGAndelArbeidsforhold::getArbeidsgiver)
                        .collect(Collectors.toSet())
                )
            );
    }


    private Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> utledBortfallendeAktiviteterSomSkalFortsattKompenseres(HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>> mellomregning,
                                                                                                                                 LocalDateTimeline<BigDecimal> antallArbeidsgivereTidslinje,
                                                                                                                                 LocalDateTimeline<Boolean> ikkeAktivTidslinje,
                                                                                                                                 Map<LocalDate, Set<Arbeidsgiver>> arbeidsgivereVedStartPrSkjæringstidspunkt,
                                                                                                                                 boolean skalDefinereIkkeYrkesaktivFraBg) {
        var resultat = new HashMap<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>>();
        for (LocalDateSegment<Boolean> periodeMedYtelse : ikkeAktivTidslinje.toSegments()) {

            var filtrertEntrySet = finnEntriesForArbeidsgivereVedSkjæringstidspunktet(mellomregning, arbeidsgivereVedStartPrSkjæringstidspunkt, skalDefinereIkkeYrkesaktivFraBg, periodeMedYtelse);
            for (Map.Entry<AktivitetIdentifikator, LocalDateTimeline<Boolean>> aktivitet : filtrertEntrySet) {
                var arbeidsforholdUtenStartPåStp = aktivitet.getValue()
                    .toSegments()
                    .stream()
                    .filter(it -> !Objects.equals(it.getFom(), periodeMedYtelse.getFom()))
                    .collect(Collectors.toList());
                var arbeidsgiverTidslinje = new LocalDateTimeline<>(arbeidsforholdUtenStartPåStp);

                var justertSegment = new LocalDateSegment<>(periodeMedYtelse.getFom().minusDays(1), periodeMedYtelse.getTom(), periodeMedYtelse.getValue());
                var yrkesaktivIPeriodeMedYtelse = arbeidsgiverTidslinje.intersection(justertSegment.getLocalDateInterval());

                if (yrkesaktivIPeriodeMedYtelse.isEmpty()) {
                    continue;
                }

                var ikkeAktivPeriode = new LocalDateTimeline<>(List.of(justertSegment)).disjoint(yrkesaktivIPeriodeMedYtelse);

                if (ikkeAktivPeriode.isEmpty()) {
                    continue;
                }


                if (ikkeAktivPeriode.toSegments().stream().noneMatch(it -> Objects.equals(it.getFom(), justertSegment.getFom()))) {
                    var arbeidsgiver = aktivitet.getKey().getArbeidsgiver();

                    if (!antallArbeidsgivereTidslinje.isEmpty()) {
                        uttakUtenSpeilingUtenErstattetArbeid(antallArbeidsgivereTidslinje, resultat, aktivitet, ikkeAktivPeriode, arbeidsgiver);
                    } else {
                        resultat.put(aktivitet.getKey(), new LocalDateTimeline<>(lagIkkeYrkesaktivSegmenter(ikkeAktivPeriode, UttakArbeidType.IKKE_YRKESAKTIV, arbeidsgiver)));
                    }
                }
            }
        }
        return resultat;
    }

    private static Set<Map.Entry<AktivitetIdentifikator, LocalDateTimeline<Boolean>>> finnEntriesForArbeidsgivereVedSkjæringstidspunktet(HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>> mellomregning, Map<LocalDate, Set<Arbeidsgiver>> arbeidsgivereVedStartPrSkjæringstidspunkt, boolean skalDefinereIkkeYrkesaktivFraBg, LocalDateSegment<Boolean> periodeMedYtelse) {
        var arbeidsgivereSomInngårIBeregningsgrunnlaget = arbeidsgivereVedStartPrSkjæringstidspunkt.get(periodeMedYtelse.getFom());
        return mellomregning.entrySet()
            .stream()
            .filter(e -> !skalDefinereIkkeYrkesaktivFraBg || arbeidsgivereSomInngårIBeregningsgrunnlaget.contains(e.getKey().getArbeidsgiver()))
            .collect(Collectors.toSet());
    }

    private void uttakUtenSpeilingUtenErstattetArbeid(LocalDateTimeline<BigDecimal> antallArbeidsgivereTidslinje, HashMap<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> resultat, Map.Entry<AktivitetIdentifikator, LocalDateTimeline<Boolean>> aktivitet, LocalDateTimeline<Boolean> ikkeAktivPeriode, Arbeidsgiver arbeidsgiver) {
        var tilkomneArbeidsgivereTidslinje = utledTilkomneArbeidsgivereTidslinje(antallArbeidsgivereTidslinje, ikkeAktivPeriode);
        var ikkeErstattetTidslinje = ikkeAktivPeriode.disjoint(tilkomneArbeidsgivereTidslinje);
        if (!ikkeErstattetTidslinje.isEmpty()) {
            resultat.put(lagNyNøkkel(aktivitet, UttakArbeidType.IKKE_YRKESAKTIV_UTEN_ERSTATNING), new LocalDateTimeline<>(lagIkkeYrkesaktivSegmenter(ikkeErstattetTidslinje, UttakArbeidType.IKKE_YRKESAKTIV_UTEN_ERSTATNING, arbeidsgiver)));
        }

        var erstattetTidslinje = ikkeAktivPeriode.intersection(tilkomneArbeidsgivereTidslinje);
        if (!erstattetTidslinje.isEmpty()) {
            resultat.put(lagNyNøkkel(aktivitet, UttakArbeidType.IKKE_YRKESAKTIV), new LocalDateTimeline<>(lagIkkeYrkesaktivSegmenter(erstattetTidslinje, UttakArbeidType.IKKE_YRKESAKTIV, arbeidsgiver)));
        }
    }

    private AktivitetIdentifikator lagNyNøkkel(Map.Entry<AktivitetIdentifikator, LocalDateTimeline<Boolean>> aktivitet, UttakArbeidType type) {
        return new AktivitetIdentifikator(type, aktivitet.getKey().getArbeidsgiver(), aktivitet.getKey().getArbeidsforhold());
    }

    private List<LocalDateSegment<WrappedArbeid>> lagIkkeYrkesaktivSegmenter(LocalDateTimeline<Boolean> tidslinje, UttakArbeidType type, Arbeidsgiver arbeidsgiver) {
        return tidslinje.toSegments()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getLocalDateInterval(),
                new WrappedArbeid(
                    new ArbeidPeriode(DatoIntervallEntitet.fra(it.getLocalDateInterval()),
                        type,
                        arbeidsgiver,
                        null,
                        Duration.ofMinutes((long) (7.5 * 60)),
                        Duration.ZERO))))
            .collect(Collectors.toList());
    }

    private void mapYrkesAktivitet(HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>> resultat, Yrkesaktivitet yrkesaktivitet) {
        var key = utledIdentifikator(yrkesaktivitet);

        var segmenter = yrkesaktivitet.getAnsettelsesPeriode()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), true))
            .toList();

        LocalDateTimeline<Boolean> aktivtsArbeidsforholdTidslinje = new LocalDateTimeline<>(segmenter, StandardCombinators::alwaysTrueForMatch);

        // Ta bort permisjoner
        var permitteringsTidslinje = PermitteringTidslinjeTjeneste.mapPermittering(yrkesaktivitet);
        aktivtsArbeidsforholdTidslinje = aktivtsArbeidsforholdTidslinje.disjoint(permitteringsTidslinje);

        var arbeidsAktivTidslinje = resultat.getOrDefault(key, LocalDateTimeline.empty());
        arbeidsAktivTidslinje = arbeidsAktivTidslinje.combine(aktivtsArbeidsforholdTidslinje, StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        resultat.put(key, arbeidsAktivTidslinje.compress());
    }


    private AktivitetIdentifikator utledIdentifikator(Yrkesaktivitet yrkesaktivitet) {
        return new AktivitetIdentifikator(UttakArbeidType.IKKE_YRKESAKTIV, yrkesaktivitet.getArbeidsgiver(), null);
    }

}
