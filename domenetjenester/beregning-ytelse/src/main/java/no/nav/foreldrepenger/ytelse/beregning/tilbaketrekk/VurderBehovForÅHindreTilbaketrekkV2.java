package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAktivitetsnøkkelV2;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

/**
 * Denne klassen skal erstatte VurderBehovForÅHindreTilbaketrekk
 * Tar inn en tidslinje over allerede utbetalt ytelse og ny beregnet ytelse
 * Sammenligner andelene og ser om det er blitt utbetalt for mye til bruker som må må tilbakekreves.
 */
public class VurderBehovForÅHindreTilbaketrekkV2 {

    private VurderBehovForÅHindreTilbaketrekkV2() {
        // Skjuler default konstruktør
    }

    static boolean skalVurdereTilbaketrekk(LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje) {

        // hvis endring i totalDagsats
        for (LocalDateSegment<BRAndelSammenligning> segment : brAndelTidslinje.toSegments()) {
            BRAndelSammenligning sammenligning = segment.getValue();
            List<BeregningsresultatAndel> originaleAndeler = sammenligning.getForrigeAndeler();
            List<BeregningsresultatAndel> revurderingAndeler = sammenligning.getBgAndeler();

            List<BRNøkkelMedAndeler> originaleAndelerSortertPåNøkkel = MapAndelerSortertPåNøkkel.map(originaleAndeler);
            List<BRNøkkelMedAndeler> revurderingAndelerSortertPåNøkkel = MapAndelerSortertPåNøkkel.map(revurderingAndeler);

            for(BRNøkkelMedAndeler originalAndel : originaleAndelerSortertPåNøkkel) {
                Optional<BRNøkkelMedAndeler> andelerIRevurderingGrunnlagMedSammeNøkkel = finnSammenligningsandelMedSammeNøkkel(originalAndel.getNøkkel(), revurderingAndelerSortertPåNøkkel);

                if (andelerIRevurderingGrunnlagMedSammeNøkkel.isPresent()) {
                    // Nøkkelen eksisterer fremdeles, må matche hver andel som tilhører nøkelen i gammelt og nytt grunnlag
                    if (nøkkelInneholderAndelerSomMåVurderes(originalAndel, andelerIRevurderingGrunnlagMedSammeNøkkel.get())) {
                        return true;
                    }

                } else {
                    // Nøkkelen har blitt borte mellom forrige behandling og denne
                    if (måAndelerSomHarBortfaltOmfordeles(originalAndel)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean nøkkelInneholderAndelerSomMåVurderes(BRNøkkelMedAndeler originalBRNøkkelMedAndeler, BRNøkkelMedAndeler revurderingBRNøkkelMedAndeler) {

        // Hvis det ligger andeler i revurderingen som ikke matcher mot noen andel i originalbehandlingen (ulik arbeidsforholdreferanse) må disse spesialhåndteres, det gjøres her.
        if (andelerIRevurderingUtenMatchIOriginalbehandlingMåVurderes(originalBRNøkkelMedAndeler, revurderingBRNøkkelMedAndeler)) {
            return true;
        }

        List<BeregningsresultatAndel> alleBrukersAndelerForNøkkelIOriginalBehandling = originalBRNøkkelMedAndeler.getBrukersAndelerTilknyttetNøkkel();
        for (BeregningsresultatAndel originalBrukersAndel : alleBrukersAndelerForNøkkelIOriginalBehandling) {
            if (andelMåVurderes(revurderingBRNøkkelMedAndeler, originalBrukersAndel)) {
                return true;
            }
        }
        return false;
    }

    private static boolean andelMåVurderes(BRNøkkelMedAndeler revurderingBRNøkkelMedAndeler, BeregningsresultatAndel originalBrukersAndel) {
        Optional<BeregningsresultatAndel> brukersAndelIRevurdering = revurderingBRNøkkelMedAndeler.getBrukersAndelMedReferanse(originalBrukersAndel.getArbeidsforholdRef());
        Optional<BeregningsresultatAndel> arbeidsgiversAndelIRevurdering = revurderingBRNøkkelMedAndeler.getArbeidsgiversAndelMedReferanse(originalBrukersAndel.getArbeidsforholdRef());
        int revurderingDagsatsBruker = dagsats(brukersAndelIRevurdering);
        int revurderingDagsatsArbeidsgiver = dagsats(arbeidsgiversAndelIRevurdering);
        int endringIDagsatsBruker = revurderingDagsatsBruker - originalBrukersAndel.getDagsats();

        return KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            revurderingDagsatsArbeidsgiver
        );
    }

    private static boolean andelerIRevurderingUtenMatchIOriginalbehandlingMåVurderes(BRNøkkelMedAndeler originalBRNøkkelMedAndeler, BRNøkkelMedAndeler revurderingBRNøkkelMedAndeler) {
        // Disse andelene har ikke matchende andel i det gamle grunnlaget
        List<BeregningsresultatAndel> andelerIRevurderingSomIkkeSvarerTilNoenIOriginalt = finnAndelerIRevurderingSomIkkeMatcherSpesifikkeArbeidsforholdIOriginaltResultat(originalBRNøkkelMedAndeler, revurderingBRNøkkelMedAndeler);

        if (!andelerIRevurderingSomIkkeSvarerTilNoenIOriginalt.isEmpty()) {
            Optional<BeregningsresultatAndel> brukersAndelPåOriginaltResultatUtenReferanse = originalBRNøkkelMedAndeler.getBrukersAndelUtenreferanse();
            return andelerMåVurderes(andelerIRevurderingSomIkkeSvarerTilNoenIOriginalt, brukersAndelPåOriginaltResultatUtenReferanse);
        }
        return false;
    }

    private static boolean andelerMåVurderes(List<BeregningsresultatAndel> andelerINyttResultatSomIkkeSvarerTilNoenIGammelt,
                                             Optional<BeregningsresultatAndel> brukersAndelPåGammeltResultatUtenReferanse) {

        // Hvis vi har en andel på gammelt grunnlag for denne nøkkelen uten referanse kan vi sjekke mot den.
        // Om vi ikke har denne må vi opprette aksjonspunkt så saksbehandler kan avgjøre hva som skal gjøres
        if (brukersAndelPåGammeltResultatUtenReferanse.isPresent()) { // NOSONAR
            return måVurdereTilkomneAndeler(andelerINyttResultatSomIkkeSvarerTilNoenIGammelt, brukersAndelPåGammeltResultatUtenReferanse.get());
        }
        return true;
    }

    private static boolean måVurdereTilkomneAndeler(List<BeregningsresultatAndel> andelerIRevurderingSomIkkeSvarerTilNoenIOriginal,
                                                    BeregningsresultatAndel brukersAndelPåOriginaltResultatUtenReferanse) {
        int aggregertArbeidsgiversDagsats = andelerIRevurderingSomIkkeSvarerTilNoenIOriginal.stream()
            .filter(a -> !a.erBrukerMottaker())
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();

        int aggregertBrukersDagsats = andelerIRevurderingSomIkkeSvarerTilNoenIOriginal.stream()
            .filter(BeregningsresultatAndel::erBrukerMottaker)
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();

        int endringIDagsatsBruker = aggregertBrukersDagsats - brukersAndelPåOriginaltResultatUtenReferanse.getDagsats();

        return KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
            endringIDagsatsBruker,
            aggregertArbeidsgiversDagsats
        );
    }

    private static List<BeregningsresultatAndel> finnAndelerIRevurderingSomIkkeMatcherSpesifikkeArbeidsforholdIOriginaltResultat(BRNøkkelMedAndeler originalBRNøkkelMedAndeler, BRNøkkelMedAndeler revurderingBRNøkkelMedAndeler) {
        List<InternArbeidsforholdRef> alleReferanserIOriginalbehandlingForNøkkel = originalBRNøkkelMedAndeler.getAlleReferanserForDenneNøkkelen();
        return revurderingBRNøkkelMedAndeler.getAlleAndelerMedRefSomIkkeFinnesIListe(alleReferanserIOriginalbehandlingForNøkkel);
    }

    private static boolean måAndelerSomHarBortfaltOmfordeles(BRNøkkelMedAndeler bortfaltAndel) {
        List<BeregningsresultatAndel> originaleBrukersAndeler = bortfaltAndel.getBrukersAndelerTilknyttetNøkkel();
        for (BeregningsresultatAndel originalBrukersAndel : originaleBrukersAndeler) {
            int revurderingDagsatsBruker = 0;
            int revurderingDagsatsArbeidsgiver = 0;
            int endringIDagsatsBruker = revurderingDagsatsBruker - originalBrukersAndel.getDagsats();

            boolean skalStoppes = KanRedusertBeløpTilBrukerDekkesAvNyRefusjon.vurder(
                endringIDagsatsBruker,
                revurderingDagsatsArbeidsgiver
            );
            if (skalStoppes) {
                return true;
            }
        }
        return false;
    }

    private static Optional<BRNøkkelMedAndeler> finnSammenligningsandelMedSammeNøkkel(BeregningsresultatAktivitetsnøkkelV2 nøkkel, List<BRNøkkelMedAndeler> liste) {
        List<BRNøkkelMedAndeler> matchendeNøkler = liste.stream()
            .filter(a -> Objects.equals(a.getNøkkel(), nøkkel))
            .collect(Collectors.toList());
        if (matchendeNøkler.size() > 1) {
            throw new IllegalStateException("Forventet å ikke finne mer enn en matchende nøkkel i liste for nøkkel " + nøkkel + " men fant " + matchendeNøkler.size());
        }
        return matchendeNøkler.stream().findFirst();
    }

    private static int dagsats(Optional<BeregningsresultatAndel> andel) {
        return andel.map(BeregningsresultatAndel::getDagsats).orElse(0);
    }
}

