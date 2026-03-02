package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.kontroll.EtterlysningOgRegisterinntekt;
import no.nav.ung.sak.kontroll.RapporterteInntekter;

/**
 * @param tidslinjeTilKontroll Tidslinje som angir hvilke perioder som skal kontrolleres i denne behandlingen
 * @param relevantForKontrollTidslinje Tidslinje som angir hvilke perioder som er relevante for kontroll for fagsaken
 * @param gjeldendeRapporterteInntekter Tidslinje med gjeldende rapporterte inntekter for perioden som skal kontrolleres
 * @param gjeldendeEtterlysningTidslinje Tidslinje med gjeldende etterlysninger og registerinntekt
 */
public record KontrollerInntektInput(LocalDateTimeline<Boolean> tidslinjeTilKontroll,
                                     LocalDateTimeline<Boolean> relevantForKontrollTidslinje,
                                     LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
                                     LocalDateTimeline<EtterlysningOgRegisterinntekt> gjeldendeEtterlysningTidslinje) {
}
