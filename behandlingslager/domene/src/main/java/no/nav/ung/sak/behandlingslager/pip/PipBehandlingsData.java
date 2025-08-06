package no.nav.ung.sak.behandlingslager.pip;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class PipBehandlingsData {
    private UUID behandlingUuid;
    private String behandligStatus;
    private String fagsakStatus;
    private String ansvarligSaksbehandler;
    private Long fagsakId;
    private String saksnummer;

    public PipBehandlingsData(UUID behandlingUuid, String behandligStatus, String ansvarligSaksbehandler, Number fagsakId,
                              String fagsakStatus, String saksnummer) {
        this.behandlingUuid = behandlingUuid;
        this.behandligStatus = behandligStatus;
        this.saksnummer = saksnummer;
        this.fagsakId = fagsakId.longValue();
        this.fagsakStatus = fagsakStatus;
        this.ansvarligSaksbehandler = ansvarligSaksbehandler;
    }

    public UUID getBehandlingUuid() {
        return behandlingUuid;
    }

    public void setBehandlingUuid(UUID behandlingUuid) {
        this.behandlingUuid = behandlingUuid;
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

    public String getSaksnummer() {
        return saksnummer;
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
                && Objects.equals(getFagsakId(), other.getFagsakId())
                && Objects.equals(getSaksnummer(), other.getSaksnummer())
                && Objects.equals(getAnsvarligSaksbehandler(), other.getAnsvarligSaksbehandler());
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandligStatus, fagsakId, saksnummer, fagsakStatus, ansvarligSaksbehandler);
    }

    @Override
    public String toString() {
        // tar ikke med ansvarligSaksbehandlinger så ikke lekker sensitive
        // personopplysninger i logg (inkl. stedslokaliserende, enhet)
        return getClass().getSimpleName() + "<"
                + "behandligStatus=" + behandligStatus + ", "
                + "saksnummer=" + saksnummer + ", "
                + "fagsakId=" + fagsakId + ", "
                + "fagsakStatus=" + fagsakStatus + ", "
                + ">"; //$NON-NLS-1$
    }

}
