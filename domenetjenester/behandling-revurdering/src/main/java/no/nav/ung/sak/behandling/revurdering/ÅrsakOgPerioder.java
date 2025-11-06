package no.nav.ung.sak.behandling.revurdering;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.List;
import java.util.Set;

public record ÅrsakOgPerioder(BehandlingÅrsakType behandlingÅrsak,
                              Set<DatoIntervallEntitet> perioder) {

    public ÅrsakOgPerioder(BehandlingÅrsakType behandlingÅrsak, DatoIntervallEntitet periode) {
        this(behandlingÅrsak, Set.of(periode));
    }
}
