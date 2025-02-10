package no.nav.ung.sak.test.util.behandling;

import java.time.LocalDate;
import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelsePeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;

/**
 * Hjelpeobjekt for å populere databasen med diverse ung data. Brukes av TestScenarioBuilder
 *
 * @param programPerioder      - perioder ungdommen er i programmet, kan være stykkevis
 * @param satser               - timeline med satser og når de gjelder. Bruk gjerne statiske hjelpebuildere fra denne klassen
 * @param uttakPerioder        - perioder med uttak, kan evt legge på gradering her
 * @param aldersvilkår         - timeline med aldersvilkår oppfylt og ikke oppfylt
 * @param ungdomsprogramvilkår - timeline med ungdomsprogramvilkår oppfylt og ikke oppfylt
 * @param fødselsdato
 */
public record UngTestscenario(
    String navn,
    List<UngdomsprogramPeriode> programPerioder,
    LocalDateTimeline<UngdomsytelseSatser> satser,
    UngdomsytelseUttakPerioder uttakPerioder,
    LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioder,
    LocalDateTimeline<Utfall> aldersvilkår,
    LocalDateTimeline<Utfall> ungdomsprogramvilkår,
    LocalDate fødselsdato) {
}


