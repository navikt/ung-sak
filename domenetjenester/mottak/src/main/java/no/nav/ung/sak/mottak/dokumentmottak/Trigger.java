package no.nav.ung.sak.mottak.dokumentmottak;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.felles.tid.DatoIntervallEntitet;

public record Trigger(DatoIntervallEntitet periode, BehandlingÅrsakType behandlingÅrsak) {
}
