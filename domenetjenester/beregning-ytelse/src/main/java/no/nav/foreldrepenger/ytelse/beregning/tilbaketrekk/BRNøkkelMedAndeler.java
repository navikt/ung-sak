package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAktivitetsnøkkelV2;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

import java.util.List;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class BRNøkkelMedAndeler {
    private BeregningsresultatAktivitetsnøkkelV2 nøkkel;
    private List<BeregningsresultatAndel> andelerTilknyttetNøkkel = new ArrayList<>();

    public BRNøkkelMedAndeler(BeregningsresultatAktivitetsnøkkelV2 nøkkel) {
        this.nøkkel = nøkkel;
    }

    public BeregningsresultatAktivitetsnøkkelV2 getNøkkel() {
        return nøkkel;
    }

    public List<BeregningsresultatAndel> getAndelerTilknyttetNøkkel() {
        return andelerTilknyttetNøkkel;
    }

    public List<InternArbeidsforholdRef> getAlleReferanserForDenneNøkkelen() {
        return andelerTilknyttetNøkkel.stream()
            .map(BeregningsresultatAndel::getArbeidsforholdRef)
            .collect(Collectors.toList());
    }

    public List<BeregningsresultatAndel> getBrukersAndelerTilknyttetNøkkel() {
        return andelerTilknyttetNøkkel.stream()
            .filter(BeregningsresultatAndel::erBrukerMottaker)
            .collect(Collectors.toList());
    }

    public List<BeregningsresultatAndel> getArbeidsgiversAndelerTilknyttetNøkkel() {
        return andelerTilknyttetNøkkel.stream()
            .filter(a -> !a.erBrukerMottaker())
            .collect(Collectors.toList());
    }

    public List<BeregningsresultatAndel> getAndelerSomHarReferanse() {
        return andelerTilknyttetNøkkel.stream()
            .filter(a -> a.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold())
            .collect(Collectors.toList());
    }

    public Optional<BeregningsresultatAndel> getBrukersAndelMedReferanse(InternArbeidsforholdRef ref) {
        List<BeregningsresultatAndel> korresponderendeAndeler = getBrukersAndelerTilknyttetNøkkel().stream()
            .filter(andel -> Objects.equals(andel.getArbeidsforholdRef(), ref))
            .collect(Collectors.toList());
        if(korresponderendeAndeler.size() > 1) {
            throw new IllegalArgumentException("Forventet å finne maks en korresponderende BeregningsresultatAndel " + nøkkel
                + ". Antall matchende aktiviteter var " + korresponderendeAndeler.size());
        }
        return korresponderendeAndeler.stream().findFirst();
    }

    public Optional<BeregningsresultatAndel> getArbeidsgiversAndelMedReferanse(InternArbeidsforholdRef ref) {
        List<BeregningsresultatAndel> korresponderendeAndeler = getArbeidsgiversAndelerTilknyttetNøkkel().stream()
            .filter(andel -> Objects.equals(andel.getArbeidsforholdRef(), ref))
            .collect(Collectors.toList());
        if(korresponderendeAndeler.size() > 1) {
            throw new IllegalArgumentException("Forventet å finne maks en korresponderende BeregningsresultatAndel " + nøkkel
                + ". Antall matchende aktiviteter var " + korresponderendeAndeler.size());
        }
        return korresponderendeAndeler.stream().findFirst();
    }

    public Optional<BeregningsresultatAndel> getBrukersAndelUtenreferanse() {
        List<BeregningsresultatAndel> korresponderendeAndeler = getBrukersAndelerTilknyttetNøkkel().stream()
            .filter(andel -> !andel.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold())
            .collect(Collectors.toList());

        if(korresponderendeAndeler.size() > 1) {
            throw new IllegalArgumentException("Forventet å finne maks en andel uten referanse for BeregningsresultatAndel " + nøkkel
                + ". Antall matchende andeler var " + korresponderendeAndeler.size());
        }
        return korresponderendeAndeler.stream().findFirst();
    }

    public Optional<BeregningsresultatAndel> getArbeidsgiversAndelUtenReferanse() {
        List<BeregningsresultatAndel> korresponderendeAndeler = getArbeidsgiversAndelerTilknyttetNøkkel().stream()
            .filter(andel -> !andel.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold())
            .collect(Collectors.toList());

        if(korresponderendeAndeler.size() > 1) {
            throw new IllegalArgumentException("Forventet å finne maks en andel uten referanse for BeregningsresultatAndel " + nøkkel
                + ". Antall matchende andeler var " + korresponderendeAndeler.size());
        }
        return korresponderendeAndeler.stream().findFirst();
    }

    void leggTilAndel(BeregningsresultatAndel andel) {
        if (matcherNøkkel(andel)) {
            andelerTilknyttetNøkkel.add(andel);
        }
    }

    private boolean matcherNøkkel(BeregningsresultatAndel andel) {
        return Objects.equals(andel.getAktivitetsnøkkelV2(), nøkkel);
    }

    public List<BeregningsresultatAndel> getAlleAndelerMedRefSomIkkeFinnesIListe(List<InternArbeidsforholdRef> referanseliste) {
        return andelerTilknyttetNøkkel.stream()
            .filter(a -> !referanseliste.contains(a.getArbeidsforholdRef()))
            .collect(Collectors.toList());
    }
}
