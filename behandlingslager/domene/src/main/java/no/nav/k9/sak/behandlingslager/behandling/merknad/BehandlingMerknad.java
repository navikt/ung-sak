package no.nav.k9.sak.behandlingslager.behandling.merknad;

import java.util.Set;

public record BehandlingMerknad (Set<BehandlingMerknadType> merknadTyper, String fritekst) {}
