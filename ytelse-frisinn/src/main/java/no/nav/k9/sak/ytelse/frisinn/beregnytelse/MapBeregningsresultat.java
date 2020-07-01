package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class MapBeregningsresultat {

    private MapBeregningsresultat() {
        // Skjul
    }

    static BeregningsresultatEntitet mapResultatFraForrige(BeregningsresultatEntitet resultat,
                                                   Optional<BeregningsresultatEntitet> resultatFraForrige,
                                                   DatoIntervallEntitet sisteSøknadsperiode) {
        BeregningsresultatEntitet.Builder builder = BeregningsresultatEntitet.builder();
        builder.medRegelInput(resultat.getRegelInput());
        builder.medRegelSporing(resultat.getRegelSporing());
        resultat.getEndringsdato().ifPresent(builder::medEndringsdato);
        BeregningsresultatEntitet nyttResultat = builder.build();
        finnPerioderForNySøknad(resultat, sisteSøknadsperiode, nyttResultat);
        finnOriginalBehandlingPerioder(resultatFraForrige, sisteSøknadsperiode, nyttResultat);
        return nyttResultat;
    }


    private static void finnOriginalBehandlingPerioder(Optional<BeregningsresultatEntitet> beregningsgrunnlagOriginalBehandling, DatoIntervallEntitet sisteSøknadsperiode, BeregningsresultatEntitet nyttResultat) {
        beregningsgrunnlagOriginalBehandling.stream()
            .flatMap(orginal -> orginal.getBeregningsresultatPerioder().stream()
                .filter(p -> p.getDagsats() > 0)
                .filter(p -> !p.getPeriode().getTomDato().isAfter(sisteSøknadsperiode.getFomDato().withDayOfMonth(1).minusDays(1))))
        .forEach(p -> kopierPeriodeOgAndeler(nyttResultat, p));
    }

    private static void finnPerioderForNySøknad(BeregningsresultatEntitet resultat, DatoIntervallEntitet sisteSøknadsperiode, BeregningsresultatEntitet nyttResultat) {
        resultat.getBeregningsresultatPerioder().stream()
            .filter(p -> p.getDagsats() > 0)
            .filter(p -> !p.getPeriode().getFomDato().isBefore(sisteSøknadsperiode.getFomDato()))
            .sorted(Comparator.comparing(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom))
            .forEach(p -> kopierPeriodeOgAndeler(nyttResultat, p));
    }

    private static void kopierPeriodeOgAndeler(BeregningsresultatEntitet nyttResultat, BeregningsresultatPeriode p) {
        BeregningsresultatPeriode nyPeriode = kopierPeriode(nyttResultat, p);
        kopierAndeler(p, nyPeriode);
    }

    private static BeregningsresultatPeriode kopierPeriode(BeregningsresultatEntitet nyttResultat, BeregningsresultatPeriode p) {
        BeregningsresultatPeriode.Builder periodeBuilder = BeregningsresultatPeriode.builder();
        periodeBuilder.medBeregningsresultatPeriodeFomOgTom(p.getBeregningsresultatPeriodeFom(), p.getBeregningsresultatPeriodeTom());
        return periodeBuilder.build(nyttResultat);
    }

    private static void kopierAndeler(BeregningsresultatPeriode p, BeregningsresultatPeriode nyPeriode) {
        p.getBeregningsresultatAndelList().forEach(a ->
            BeregningsresultatAndel.builder()
                .medDagsats(a.getDagsats())
                .medDagsatsFraBg(a.getDagsatsFraBg())
                .medBrukerErMottaker(a.erBrukerMottaker())
                .medInntektskategori(a.getInntektskategori())
                .medStillingsprosent(a.getStillingsprosent())
                .medUtbetalingsgrad(a.getUtbetalingsgrad())
                .medArbeidsgiver(a.getArbeidsgiver().orElse(null))
                .medAktivitetStatus(a.getAktivitetStatus())
                .medArbeidsforholdRef(a.getArbeidsforholdRef())
                .medArbeidsforholdType(a.getArbeidsforholdType())
                .build(nyPeriode));
    }

}
