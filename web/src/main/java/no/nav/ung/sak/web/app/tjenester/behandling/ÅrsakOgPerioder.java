package no.nav.ung.sak.web.app.tjenester.behandling;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.typer.Periode;

import java.util.List;

public record ÅrsakOgPerioder(BehandlingÅrsakType behandlingÅrsakType, List<Periode> perioder) {
}
