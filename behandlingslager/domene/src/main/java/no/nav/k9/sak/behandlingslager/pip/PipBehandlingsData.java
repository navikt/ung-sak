package no.nav.k9.sak.behandlingslager.pip;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

public class PipBehandlingsData {
    private String behandligStatus;
    private String fagsakStatus;
    private String ansvarligSaksbehandler;
    private Long fagsakId;

    public PipBehandlingsData(String behandligStatus, String ansvarligSaksbehandler, BigInteger fagsakId, String fagsakStatus) {
        this.behandligStatus = behandligStatus;
        this.fagsakId = fagsakId.longValue();
        this.fagsakStatus = fagsakStatus;
        this.ansvarligSaksbehandler = ansvarligSaksbehandler;
    }

    public String getBehandligStatus() {
        return behandligStatus;
    }

    public void setBehandligStatus(String behandligStatus) {
        this.behandligStatus = behandligStatus;
    }

    public String getFagsakStatus() {
        return fagsakStatus;
    }

    public void setFagsakStatus(String fagsakStatus) {
        this.fagsakStatus = fagsakStatus;
    }

    public Optional<String> getAnsvarligSaksbehandler() {
        return Optional.ofNullable(ansvarligSaksbehandler);
    }

    public void setAnsvarligSaksbehandler(String ansvarligSaksbehandler) {
        this.ansvarligSaksbehandler = ansvarligSaksbehandler;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof PipBehandlingsData)) {
            return false;
        }
        PipBehandlingsData other = (PipBehandlingsData) object;
        return Objects.equals(getBehandligStatus(), other.getBehandligStatus())
            && Objects.equals(getFagsakStatus(), other.getFagsakStatus())
            && Objects.equals(getAnsvarligSaksbehandler(), other.getAnsvarligSaksbehandler());
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandligStatus, fagsakId, fagsakStatus, ansvarligSaksbehandler);
    }

    @Override
    public String toString() {
        // tar ikke med ansvarligSaksbehandlinger så ikke lekker sensitive personopplysninger i logg (inkl. stedslokaliserende, enhet)
        return getClass().getSimpleName() + "<"
            + "behandligStatus=" + behandligStatus + ", "
            + "fagsakId=" + fagsakId + ", "
            + "fagsakStatus=" + fagsakStatus + ", "
            + ">"; //$NON-NLS-1$
    }

}
