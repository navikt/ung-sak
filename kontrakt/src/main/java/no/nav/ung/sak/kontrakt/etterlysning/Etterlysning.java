package no.nav.ung.sak.kontrakt.etterlysning;

import no.nav.ung.kodeverk.forhåndsvarsel.EtterlysningStatus;
import no.nav.ung.kodeverk.forhåndsvarsel.EtterlysningType;
import no.nav.ung.sak.typer.Periode;

import java.util.UUID;

public record Etterlysning(EtterlysningStatus status, EtterlysningType type, Periode periode, UUID eksternReferanse) {
}
