package no.nav.k9.sak.behandling.aksjonspunkt;

import java.util.Objects;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

public class AksjonspunktUtlederInput {

    private BehandlingReferanse ref;

    /**
     * @deprecated Ikke bruk i prod kode - kun mens vi endrer kodebasen
     */
    @Deprecated(forRemoval = true)
    public AksjonspunktUtlederInput(Behandling behandling) {
        Objects.requireNonNull(behandling, "behandling");
        this.ref = BehandlingReferanse.fra(behandling);
    }

    /**
     * @deprecated Ikke bruk i prod kode - kun mens vi endrer kodebasen
     */
    @Deprecated(forRemoval = true)
    public AksjonspunktUtlederInput(Behandling behandling, Skjæringstidspunkt skjæringstidspunkt) {
        Objects.requireNonNull(behandling, "behandling");
        Objects.requireNonNull(skjæringstidspunkt, "skjæringstidspunkt");
        this.ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
    }

    public AksjonspunktUtlederInput(BehandlingReferanse ref) {
        Objects.requireNonNull(ref, "ref");
        this.ref = ref;
    }

    public Long getBehandlingId( ) {
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

    public Skjæringstidspunkt getSkjæringstidspunkt() {
        return ref.getSkjæringstidspunkt();
    }

    public Saksnummer getSaksnummer() {
        return ref.getSaksnummer();
    }
}
