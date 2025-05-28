package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.ytelse.EtterlysningOgRegisterinntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

public record KontrollerInntektInput(LocalDateTimeline<Boolean> relevantTidslinje,
                                     LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
                                     LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje) {
}
