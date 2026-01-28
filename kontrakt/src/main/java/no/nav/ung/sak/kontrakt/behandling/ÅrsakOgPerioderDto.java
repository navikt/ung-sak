package no.nav.ung.sak.kontrakt.behandling;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.felles.typer.Periode;

import java.util.List;

public record ÅrsakOgPerioderDto(BehandlingÅrsakType årsak, List<Periode> perioder) {
}
