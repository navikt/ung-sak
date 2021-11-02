package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.util.Objects;

import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class VurderStatusInput {
    private OpptjeningAktivitetType type;
    private BehandlingReferanse behandlingReferanse;
    private DatoIntervallEntitet opptjeningsperiode;
    private Yrkesaktivitet registerAktivitet;

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

    public DatoIntervallEntitet getOpptjeningsperiode() {
        return opptjeningsperiode;
    }

    public void setOpptjeningsperiode(DatoIntervallEntitet opptjeningsperiode) {
        this.opptjeningsperiode = opptjeningsperiode;
    }

    public Yrkesaktivitet getRegisterAktivitet() {
        return registerAktivitet;
    }

    public void setRegisterAktivitet(Yrkesaktivitet yrkesaktivitet) {
        this.registerAktivitet = yrkesaktivitet;
    }
}
