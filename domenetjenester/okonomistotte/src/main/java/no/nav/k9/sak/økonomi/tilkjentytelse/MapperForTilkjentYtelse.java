package no.nav.k9.sak.økonomi.tilkjentytelse;

import java.time.Year;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.oppdrag.kontrakt.kodeverk.Inntektskategori;
import no.nav.k9.oppdrag.kontrakt.kodeverk.SatsType;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelseAndelV1;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelsePeriodeV1;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class MapperForTilkjentYtelse {

    private static final Logger logger = LoggerFactory.getLogger(MapperForTilkjentYtelse.class);

    private final SatsType satsType;

    public MapperForTilkjentYtelse(FagsakYtelseType ytelseType) {
        satsType = ytelseType == FagsakYtelseType.OMSORGSPENGER ? SatsType.DAG7 : SatsType.DAG;
    }

    public List<TilkjentYtelsePeriodeV1> mapTilkjentYtelse(BeregningsresultatEntitet beregningsresultat) {
        if (beregningsresultat == null) {
            return Collections.emptyList();
        }
        return beregningsresultat.getBeregningsresultatPerioder()
            .stream()
            .map(this::mapPeriode)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private TilkjentYtelsePeriodeV1 mapPeriode(BeregningsresultatPeriode periode) {
        List<BeregningsresultatAndel> brAndeler = periode.getBeregningsresultatAndelList();
        List<TilkjentYtelseAndelV1> andeler = mapAndeler(brAndeler);
        if (andeler.isEmpty()) {
            logger.info("Periode {}-{} hadde ingen beløp over 0 og ble ignorert", periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom());
            return null;
        }
        return new TilkjentYtelsePeriodeV1(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom(), andeler);
    }

    private List<TilkjentYtelseAndelV1> mapAndeler(List<BeregningsresultatAndel> brAndeler) {
        //unngår å sende arbeidsgiver-id når det kan unngås, siden det er delvis lokaliserende informasjon
        boolean sendArbeidsgiverIdForBrukersAndeler = harAndelerTilBrukerForskjelligeArbeidsgivere(brAndeler);

        return brAndeler.stream()
            .map(brAndel -> mapAndel(brAndel, sendArbeidsgiverIdForBrukersAndeler))
            .filter(andel -> andel.getSatsBeløp() != 0)
            .collect(Collectors.toList());
    }

    private boolean harAndelerTilBrukerForskjelligeArbeidsgivere(Collection<BeregningsresultatAndel> andeler) {
        return andeler.stream()
            .filter(BeregningsresultatAndel::erBrukerMottaker)
            .map(BeregningsresultatAndel::getArbeidsgiver)
            .distinct()
            .count() > 1;
    }

    private TilkjentYtelseAndelV1 mapAndel(BeregningsresultatAndel andel, boolean sendArbeidsgiverIdForBrukersAndeler) {
        TilkjentYtelseAndelV1 resultat = mapAndelUtenFeriepenger(andel, sendArbeidsgiverIdForBrukersAndeler);
        resultat.medUtbetalingsgrad(andel.getUtbetalingsgrad());
        var feriepenger = andel.getFeriepengerÅrsbeløp();
        if (feriepenger != null) {
            Year år = Year.from(andel.getFom());
            long beløp = feriepenger.getVerdi().longValue();
            resultat.leggTilFeriepenger(år, beløp);
        }
        return resultat;
    }

    private TilkjentYtelseAndelV1 mapAndelUtenFeriepenger(BeregningsresultatAndel andel, boolean sendArbeidsgiverIdForBrukersAndeler) {
        Inntektskategori inntektskategori = MapperForInntektskategori.mapInntektskategori(andel.getInntektskategori());
        int dagsats = andel.getDagsats();

        TilkjentYtelseAndelV1 andelV1 = andel.erBrukerMottaker()
            ? TilkjentYtelseAndelV1.tilBruker(inntektskategori, dagsats, satsType)
            : TilkjentYtelseAndelV1.refusjon(inntektskategori, dagsats, satsType);

        Optional<Arbeidsgiver> arbeidsgiverOpt = andel.getArbeidsgiver();
        boolean brukArbeidsgiverIdHvisFinnes = !andel.erBrukerMottaker() || sendArbeidsgiverIdForBrukersAndeler;
        if (brukArbeidsgiverIdHvisFinnes && arbeidsgiverOpt.isPresent()) {
            if (andel.erArbeidsgiverPrivatperson()) {
                andelV1.medArbeidsgiverAktørId(arbeidsgiverOpt.get().getIdentifikator());
            } else {
                andelV1.medArbeidsgiverOrgNr(arbeidsgiverOpt.get().getIdentifikator());
                andelV1.medArbeidsforholdId(andel.getArbeidsforholdRef().getReferanse());
            }
        }
        return andelV1;
    }
}
