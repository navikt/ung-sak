package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.Kopimaskin;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAktivitetsnøkkelV2;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

class OmfordelUtbetaltYtelseV2 {
    private OmfordelUtbetaltYtelseV2() {
        // skjul public constructor
    }

    static List<BeregningsresultatAndel.Builder> omfordel(List<BeregningsresultatAndel> originaleAndeler, List<BeregningsresultatAndel> revurderingAndeler) {

        List<BRNøkkelMedAndeler> originaleAndelerSortertPåNøkkel = MapAndelerSortertPåNøkkel.map(originaleAndeler);
        List<BRNøkkelMedAndeler> revurderingAndelerSortertPåNøkkel = MapAndelerSortertPåNøkkel.map(revurderingAndeler);

        List<BeregningsresultatAndel.Builder> list = new ArrayList<>();

        for (BRNøkkelMedAndeler revurderingNøkkelMedAndeler : revurderingAndelerSortertPåNøkkel) {
            Optional<BRNøkkelMedAndeler> originalNøkkelMedAndeler = finnSammenligningsandelMedSammeNøkkel(revurderingNøkkelMedAndeler.getNøkkel(), originaleAndelerSortertPåNøkkel);
            if (originalNøkkelMedAndeler.isPresent()) {
                list.addAll(omfordelAndelerMellomLikeNøkler(originalNøkkelMedAndeler.get(), revurderingNøkkelMedAndeler));
            } else {
                // Nøkkelen er ny, og vi har ingen andeler å overføre fra
                list.addAll(byggAndelerForTilkommetNøkkel(revurderingNøkkelMedAndeler));
            }
        }
        return list;
    }

    private static List<BeregningsresultatAndel.Builder> byggAndelerForTilkommetNøkkel(BRNøkkelMedAndeler revurderingNøkkelMedAndeler) {
        List<BeregningsresultatAndel.Builder> list = new ArrayList<>();
        for (BeregningsresultatAndel revurderingAndel : revurderingNøkkelMedAndeler.getAndelerTilknyttetNøkkel()) {
            int dagsats = revurderingAndel.getDagsats();
            boolean erBrukerMottaker = revurderingAndel.erBrukerMottaker();
            if (dagsats != 0) {
                BeregningsresultatAndel.Builder builder = BeregningsresultatAndel.builder(Kopimaskin.deepCopy(revurderingAndel))
                    .medDagsats(dagsats)
                    .medDagsatsFraBg(revurderingAndel.getDagsatsFraBg());
                list.add(builder);
            } else if (erBrukerMottaker) {
                BeregningsresultatAndel.Builder builder = BeregningsresultatAndel.builder(Kopimaskin.deepCopy(revurderingAndel))
                    .medDagsats(dagsats)
                    .medDagsatsFraBg(revurderingAndel.getDagsatsFraBg());
                list.add(builder);
            }
        }
        return list;
    }

    private static List<BeregningsresultatAndel.Builder> omfordelAndelerMellomLikeNøkler(BRNøkkelMedAndeler originalNøkkelMedAndeler, BRNøkkelMedAndeler revurderingNøkkelMedAndeler) {
        List<BeregningsresultatAndel.Builder> list = new ArrayList<>();

        // Omfordeler alle revurderingsandeler som ikke har en referanse
        list.addAll(omfordelAndelerUtenReferanse(revurderingNøkkelMedAndeler, originalNøkkelMedAndeler));

        for (BeregningsresultatAndel revurderingAndel : revurderingNøkkelMedAndeler.getAndelerSomHarReferanse()) {
            int reberegnetDagsats = getReberegnetDagsats(revurderingAndel,  revurderingNøkkelMedAndeler, originalNøkkelMedAndeler);
            if (reberegnetDagsats != 0) {
                BeregningsresultatAndel.Builder builder = BeregningsresultatAndel.builder(Kopimaskin.deepCopy(revurderingAndel))
                    .medDagsats(reberegnetDagsats)
                    .medDagsatsFraBg(revurderingAndel.getDagsatsFraBg());
                list.add(builder);
            } else if (revurderingAndel.erBrukerMottaker()) {
                BeregningsresultatAndel.Builder builder = BeregningsresultatAndel.builder(Kopimaskin.deepCopy(revurderingAndel))
                    .medDagsats(reberegnetDagsats)
                    .medDagsatsFraBg(revurderingAndel.getDagsatsFraBg());
                list.add(builder);
            }
        }
        return list;
    }

    private static int getReberegnetDagsats(BeregningsresultatAndel revurderingAndel, BRNøkkelMedAndeler revurderingNøkkelMedAndeler, BRNøkkelMedAndeler originalNøkkelMedAndeler) {
        int originalBrukersDagsats = originalNøkkelMedAndeler.getBrukersAndelMedReferanse(revurderingAndel.getArbeidsforholdRef())
            .map(BeregningsresultatAndel::getDagsats)
            .orElse(0);
        int reberegnetDagsats;
        if (revurderingAndel.erBrukerMottaker()) {
            int revurderingArbeidsgiverDagsats = revurderingNøkkelMedAndeler.getArbeidsgiversAndelMedReferanse(revurderingAndel.getArbeidsforholdRef())
                .map(BeregningsresultatAndel::getDagsats)
                .orElse(0);
            reberegnetDagsats = OmfordelDagsats.beregnDagsatsBruker(revurderingAndel.getDagsats(), revurderingArbeidsgiverDagsats, originalBrukersDagsats);
        } else {
            int revurderingBrukersDagsats = revurderingNøkkelMedAndeler.getBrukersAndelMedReferanse(revurderingAndel.getArbeidsforholdRef())
                .map(BeregningsresultatAndel::getDagsats)
                .orElse(0);
            reberegnetDagsats = OmfordelDagsats.beregnDagsatsArbeidsgiver(revurderingAndel.getDagsats(), revurderingBrukersDagsats, originalBrukersDagsats);
        }
        return reberegnetDagsats;
    }

    private static List<BeregningsresultatAndel.Builder> omfordelAndelerUtenReferanse(BRNøkkelMedAndeler revurderingNøkkelMedAndeler, BRNøkkelMedAndeler originalNøkkelMedAndeler) {
        List<InternArbeidsforholdRef> revurderingReferanser = revurderingNøkkelMedAndeler.getAlleReferanserForDenneNøkkelen();
        List<BeregningsresultatAndel.Builder> list = new ArrayList<>();
        List<BeregningsresultatAndel> andelerMedRefSomIkkeFinnesIRevurdering = originalNøkkelMedAndeler.getAlleAndelerMedRefSomIkkeFinnesIListe(revurderingReferanser);

        Optional<BeregningsresultatAndel> brukersAndelUtenreferanse = revurderingNøkkelMedAndeler.getBrukersAndelUtenreferanse();
        Optional<BeregningsresultatAndel> arbeidsgiversAndelUtenReferanse = revurderingNøkkelMedAndeler.getArbeidsgiversAndelUtenReferanse();

        if (brukersAndelUtenreferanse.isPresent()) {
            int omberegnetDagsats = beregnDagsatsBrukerAndelUtenReferanse(originalNøkkelMedAndeler, andelerMedRefSomIkkeFinnesIRevurdering ,
                brukersAndelUtenreferanse.get(), arbeidsgiversAndelUtenReferanse);
            BeregningsresultatAndel.Builder builder = BeregningsresultatAndel.builder(Kopimaskin.deepCopy(brukersAndelUtenreferanse.get()))
                .medDagsats(omberegnetDagsats)
                .medDagsatsFraBg(brukersAndelUtenreferanse.get().getDagsatsFraBg());
            list.add(builder);
        }
        if (arbeidsgiversAndelUtenReferanse.isPresent()) {
            int omberegnetDagsats = beregnDagsatsAGUtenReferanse(originalNøkkelMedAndeler, andelerMedRefSomIkkeFinnesIRevurdering ,
                arbeidsgiversAndelUtenReferanse.get(), brukersAndelUtenreferanse);
            if (omberegnetDagsats != 0) {
                BeregningsresultatAndel.Builder builder = BeregningsresultatAndel.builder(Kopimaskin.deepCopy(arbeidsgiversAndelUtenReferanse.get()))
                    .medDagsats(omberegnetDagsats)
                    .medDagsatsFraBg(arbeidsgiversAndelUtenReferanse.get().getDagsatsFraBg());
                list.add(builder);
            }
        }
        return list;
    }

    private static int beregnDagsatsAGUtenReferanse(BRNøkkelMedAndeler originalNøkkelMedAndeler,
                                                    List<BeregningsresultatAndel> alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering,
                                                    BeregningsresultatAndel arbeidsgiversAndelUtenReferanse,
                                                    Optional<BeregningsresultatAndel> brukersAndelUtenreferanse) {
        int originalDagsatsBrukersAndelUtenMatchendeRef = alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering.stream()
            .filter(BeregningsresultatAndel::erBrukerMottaker)
            .mapToInt(BeregningsresultatAndel::getDagsats)
            .sum();
        int originalDagsatsBrukersAndelUtenRef = originalNøkkelMedAndeler.getBrukersAndelUtenreferanse()
            .map(BeregningsresultatAndel::getDagsats)
            .orElse(0);
        int originalDagsatsBrukerTotal = originalDagsatsBrukersAndelUtenRef + originalDagsatsBrukersAndelUtenMatchendeRef;
        int revurderingBrukersDagsats = brukersAndelUtenreferanse
            .map(BeregningsresultatAndel::getDagsats)
            .orElse(0);
        return OmfordelDagsats.beregnDagsatsArbeidsgiver(arbeidsgiversAndelUtenReferanse.getDagsats(), revurderingBrukersDagsats, originalDagsatsBrukerTotal);


    }
    private static int beregnDagsatsBrukerAndelUtenReferanse(BRNøkkelMedAndeler originalNøkkelMedAndeler,
                                                             List<BeregningsresultatAndel> alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering,
                                                             BeregningsresultatAndel brukersAndelUtenreferanse,
                                                             Optional<BeregningsresultatAndel> arbeidsgiversAndelUtenReferanse) {
        int originalDagsatsAndelUtenReferanse = originalNøkkelMedAndeler.getBrukersAndelUtenreferanse()
            .map(BeregningsresultatAndel::getDagsats)
            .orElse(0);
        List<BeregningsresultatAndel> originaleBrukersAndelSomIkkeMatcherRevurderingAndeler = alleOriginaleAndelerMedReferanseSomIkkeFinnesIRevurdering.stream()
            .filter(BeregningsresultatAndel::erBrukerMottaker)
            .collect(Collectors.toList());
        int originalDagsatsAndelerUtenMatchendeRef = originaleBrukersAndelSomIkkeMatcherRevurderingAndeler.stream().mapToInt(BeregningsresultatAndel::getDagsats).sum();
        int totalOriginalBrukersDagsats = originalDagsatsAndelUtenReferanse + originalDagsatsAndelerUtenMatchendeRef;
        int revurderingBrukerDagsats = brukersAndelUtenreferanse.getDagsats();
        int revurderingDagsatsArbeidsgiver = arbeidsgiversAndelUtenReferanse
            .map(BeregningsresultatAndel::getDagsats)
            .orElse(0);
        return OmfordelDagsats.beregnDagsatsBruker(revurderingBrukerDagsats, revurderingDagsatsArbeidsgiver, totalOriginalBrukersDagsats);
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

}
