package no.nav.ung.sak.test.util.behandling.aktivitetspenger;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Hjelpeobjekt for å populere databasen med diverse aktivitetspenger-data. Brukes av TestScenarioBuilder
 */
public record AktivitetspengerTestScenario(
    String navn,
    List<Periode> søknadsperioder,
    LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioder,
    LocalDateTimeline<Utfall> aldersvilkår,
    LocalDate fødselsdato,
    Set<Trigger> behandlingTriggere,
    List<PersonInformasjon> barn,
    LocalDate dødsdato,
    LocalDateTimeline<KontrollertInntektPeriode> kontrollerInntektPerioder) {
}


