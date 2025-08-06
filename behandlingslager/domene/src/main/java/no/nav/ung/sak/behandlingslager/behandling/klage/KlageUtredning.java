package no.nav.ung.sak.behandlingslager.behandling.klage;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.kodeverk.BehandlingTypeKodeverdiConverter;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Entity(name = "KlageUtredning")
@Table(name = "KLAGE_UTREDNING")
public class KlageUtredning extends BaseEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KLAGE_UTREDNING")
    private Long id;

    @Column(name = "klage_behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @Column(name = "paaklagd_ekstern_uuid")
    private UUID påKlagdBehandlingRef;

    @Convert(converter = BehandlingTypeKodeverdiConverter.class)
    @Column(name = "paaklagd_behandling_type")
    private BehandlingType påKlagdBehandlingType;

    @Column(name = "oppr_behandlende_enhet")
    private String opprinneligBehandlendeEnhet;

    @Column(name = "godkjent_av_medunderskriver", nullable = false)
    private boolean godkjentAvMedunderskriver;

//    @OneToOne(cascade = CascadeType.PERSIST)
//    @JoinColumn(name = "part_id")
//    private PartEntitet klagendePart;

    public KlageUtredning() {
        // Hibernate
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<UUID> getPåKlagBehandlingRef() {
        return Optional.ofNullable(påKlagdBehandlingRef);
    }

    public void setPåKlagdBehandlingRef(UUID påKlagdBehandlingRef) {
        this.påKlagdBehandlingRef = påKlagdBehandlingRef;
    }

    public Optional<BehandlingType> getPåKlagdBehandlingType() {
        return Optional.ofNullable(påKlagdBehandlingType);
    }

    public void setPåKlagdBehandlingType(BehandlingType påKlagdBehandlingType) {
        this.påKlagdBehandlingType = påKlagdBehandlingType;
    }

    public boolean isGodkjentAvMedunderskriver() {
        return godkjentAvMedunderskriver;
    }

    public void setGodkjentAvMedunderskriver(boolean godkjentAvMedunderskriver) {
        this.godkjentAvMedunderskriver = godkjentAvMedunderskriver;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof KlageUtredning)) {
            return false;
        }
        KlageUtredning other = (KlageUtredning) obj;
        return Objects.equals(this.id, other.id) //Skal det sammenliknes på id?
            && Objects.equals(this.behandlingId, other.behandlingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, behandlingId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + //$NON-NLS-1$
            (id != null ? "id=" + id + ", " : "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + "klageBehandling=" + behandlingId + ", " //$NON-NLS-1$ //$NON-NLS-2$
            + ">"; //$NON-NLS-1$
    }

    public String getOpprinneligBehandlendeEnhet() {
        return opprinneligBehandlendeEnhet;
    }

//    public Optional<PartEntitet> getKlagendePart() {
//        return Optional.ofNullable(klagendePart);
//    }
//
//    public void setKlagendePart(PartEntitet klagendePart) {
//        this.klagendePart = klagendePart;
//    }

    public static class Builder {
        private KlageUtredning klageUtredningMal;

        public Builder() {
            klageUtredningMal = new KlageUtredning();
        }

        public Builder medOpprinneligBehandlendeEnhet(String opprinneligBehandlendeEnhet) {
            klageUtredningMal.opprinneligBehandlendeEnhet = opprinneligBehandlendeEnhet;
            return this;
        }

        public Builder medKlageBehandling(Behandling klageBehandling) {
            klageUtredningMal.behandlingId = klageBehandling.getId();
            return this;
        }

        public Builder medPåKlagdBehandlingRef(UUID påklagetBehandlingRef) {
            klageUtredningMal.påKlagdBehandlingRef = påklagetBehandlingRef;
            return this;
        }

        public Builder medPåKlagdBehandlingType(BehandlingType påklagetBehandlingType) {
            klageUtredningMal.påKlagdBehandlingType = påklagetBehandlingType;
            return this;
        }

        public KlageUtredning build() {
            verifyStateForBuild();
            return klageUtredningMal;
        }

        public void verifyStateForBuild() {
            Objects.requireNonNull(klageUtredningMal.behandlingId, "KlageBehandling");
        }
    }
}
