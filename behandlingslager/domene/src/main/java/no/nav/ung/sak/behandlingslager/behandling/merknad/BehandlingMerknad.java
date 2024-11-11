package no.nav.ung.sak.behandlingslager.behandling.merknad;

import java.util.Set;

public record BehandlingMerknad (Set<BehandlingMerknadType> merknadTyper, String fritekst) {}
