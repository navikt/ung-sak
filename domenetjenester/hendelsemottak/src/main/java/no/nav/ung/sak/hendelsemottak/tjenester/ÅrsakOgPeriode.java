package no.nav.ung.sak.hendelsemottak.tjenester;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

public record ÅrsakOgPeriode(BehandlingÅrsakType behandlingÅrsak,
                             DatoIntervallEntitet periode) {
}
