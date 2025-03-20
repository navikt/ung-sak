package no.nav.ung.sak.kontrakt.etterlysning;

import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.typer.Periode;

import java.util.UUID;

public record Etterlysning(EtterlysningStatus status, EtterlysningType type, Periode periode, UUID eksternReferanse) {
}
