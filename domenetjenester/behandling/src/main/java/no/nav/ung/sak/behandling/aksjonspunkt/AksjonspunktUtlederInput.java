package no.nav.ung.sak.behandling.aksjonspunkt;

import java.util.Objects;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

public class AksjonspunktUtlederInput {

    private BehandlingReferanse ref;

    public AksjonspunktUtlederInput(BehandlingReferanse ref) {
        Objects.requireNonNull(ref, "ref");
        this.ref = ref;
    }

    public Long getBehandlingId() {
        return ref.getBehandlingId();
    }

    public AktørId getAktørId() {
        return ref.getAktørId();
    }

    public FagsakYtelseType getYtelseType() {
        return ref.getFagsakYtelseType();
    }

    public BehandlingType getBehandlingType() {
        return ref.getBehandlingType();
    }

    public BehandlingReferanse getRef() {
        return ref;
    }

    public Saksnummer getSaksnummer() {
        return ref.getSaksnummer();
    }
}
