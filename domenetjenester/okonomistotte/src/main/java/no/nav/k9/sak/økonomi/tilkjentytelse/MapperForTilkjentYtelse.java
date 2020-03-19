package no.nav.k9.sak.økonomi.tilkjentytelse;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori;
import no.nav.k9.oppdrag.kontrakt.kodeverk.SatsType;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseAndelV1;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelsePeriodeV1;
import no.nav.k9.sak.typer.Arbeidsgiver;

class MapperForTilkjentYtelse {

    private MapperForTilkjentYtelse() {
        //hindrer instansiering, som gjør sonarqube glad
    }

    static List<TilkjentYtelsePeriodeV1> mapTilkjentYtelse(BeregningsresultatEntitet beregningsresultat) {
        return beregningsresultat.getBeregningsresultatPerioder()
            .stream()
            .map(MapperForTilkjentYtelse::mapPeriode)
            .collect(Collectors.toList());
    }

    private static TilkjentYtelsePeriodeV1 mapPeriode(BeregningsresultatPeriode periode) {
        List<TilkjentYtelseAndelV1> andeler = periode.getBeregningsresultatAndelList()
            .stream()
            .map(MapperForTilkjentYtelse::mapAndel)
            .collect(Collectors.toList());
        return new TilkjentYtelsePeriodeV1(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom(), andeler);
    }

    private static TilkjentYtelseAndelV1 mapAndel(BeregningsresultatAndel andel) {
        TilkjentYtelseAndelV1 resultat = mapAndelUtenFeriepenger(andel);
        resultat.medUtbetalingsgrad(andel.getUtbetalingsgrad());
        for (BeregningsresultatFeriepengerPrÅr feriepengerPrÅr : andel.getBeregningsresultatFeriepengerPrÅrListe()) {
            Year år = Year.of(feriepengerPrÅr.getOpptjeningsår().getYear());
            long beløp = feriepengerPrÅr.getÅrsbeløp().getVerdi().longValue();
            resultat.leggTilFeriepenger(år, beløp);
        }
        return resultat;
    }

    private static TilkjentYtelseAndelV1 mapAndelUtenFeriepenger(BeregningsresultatAndel andel) {
        Inntektskategori inntektskategori = MapperForInntektskategori.mapInntektskategori(andel.getInntektskategori());
        int dagsats = andel.getDagsats();
        SatsType satsType = SatsType.DAG;


        TilkjentYtelseAndelV1 andelV1 = andel.erBrukerMottaker()
            ? TilkjentYtelseAndelV1.tilBruker(inntektskategori, dagsats, satsType)
            : TilkjentYtelseAndelV1.refusjon(inntektskategori, dagsats, satsType);

        Optional<Arbeidsgiver> arbeidsgiverOpt = andel.getArbeidsgiver();
        if (!andel.erBrukerMottaker() && arbeidsgiverOpt.isPresent()) {
            if (andel.erArbeidsgiverPrivatperson()) {
                andelV1.medArbeidsgiverAktørId(arbeidsgiverOpt.get().getIdentifikator());
            } else {
                andelV1.medArbeidsgiverOrgNr(arbeidsgiverOpt.get().getIdentifikator());
            }
        }
        return andelV1;
    }
}
