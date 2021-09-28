package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.util.Objects;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;

public class VurderStatusInput {
    private OpptjeningAktivitetType type;
    private BehandlingReferanse behandlingReferanse;

    public VurderStatusInput(OpptjeningAktivitetType type, BehandlingReferanse behandlingReferanse) {
        this.type = Objects.requireNonNull(type);
        this.behandlingReferanse = Objects.requireNonNull(behandlingReferanse);
    }

    public OpptjeningAktivitetType getType() {
        return type;
    }

    public BehandlingReferanse getBehandlingReferanse() {
        return behandlingReferanse;
    }
}
