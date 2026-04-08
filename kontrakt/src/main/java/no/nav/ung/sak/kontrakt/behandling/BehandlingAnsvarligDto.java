package no.nav.ung.sak.kontrakt.behandling;

public record BehandlingAnsvarligDto(String ansvarligSaksbehandler,
                                     String ansvarligBeslutter,
                                     String behandlendeEnhetId,
                                     String behandlendeEnhetNavn,
                                     boolean toTrinnsBehandling) {
}
