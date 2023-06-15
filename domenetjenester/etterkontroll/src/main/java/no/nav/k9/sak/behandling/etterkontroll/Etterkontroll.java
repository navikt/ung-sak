package no.nav.k9.sak.behandling.etterkontroll;


import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;

@Entity(name = "Etterkontroll")
@Table(name = "ETTERKONTROLL")
public class Etterkontroll extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ETTERKONTROLL")
    private Long id;

    @Column(name = "fagsak_id", nullable = false, updatable = false)
    private Long fagsakId;

    //I praksis non-nullable, men må være slik pga gammel data
    @Column(name = "behandling_id", nullable = true, updatable = false)
    private Long behandlingId;

    @DiffIgnore
    @Column(name = "kontroll_tid", nullable = false)
    private LocalDateTime kontrollTidspunkt; // NOSONAR

    @Convert(converter = EtterkontrollKodeverdiConverter.class)
    @Column(name = "kontroll_type", nullable = false)
    private KontrollType kontrollType;

    @Column(name = "behandlet", nullable = false)
    private boolean erBehandlet = false;

    Etterkontroll() {
        // hibernarium
    }

    public Long getId() {
        return id;
    }


    public void setErBehandlet(boolean erBehandlet) {
        this.erBehandlet = erBehandlet;
    }

    public boolean isBehandlet() {
        return erBehandlet;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public KontrollType getKontrollType() {
        return kontrollType;
    }

    public LocalDateTime getKontrollTidspunkt() {
        return kontrollTidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Etterkontroll that = (Etterkontroll) o;
        return erBehandlet == that.erBehandlet && Objects.equals(fagsakId, that.fagsakId) && Objects.equals(behandlingId, that.behandlingId) && Objects.equals(kontrollTidspunkt, that.kontrollTidspunkt) && kontrollType == that.kontrollType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fagsakId, behandlingId, kontrollTidspunkt, kontrollType, erBehandlet);
    }

    public static class Builder {
        private Etterkontroll etterkontrollKladd;

        public Builder(Long fagsakId) {
            Objects.requireNonNull(fagsakId, "fagsakId");
            etterkontrollKladd = new Etterkontroll();
            this.etterkontrollKladd.fagsakId = fagsakId;
        }

        public Builder(Behandling behandling) {
            Objects.requireNonNull(behandling, "behandling");
            etterkontrollKladd = new Etterkontroll();
            this.etterkontrollKladd.fagsakId = Objects.requireNonNull(behandling.getFagsakId());
            this.etterkontrollKladd.behandlingId = behandling.getId();
        }

        public Etterkontroll build() {
            // TODO: valider
            Objects.requireNonNull(etterkontrollKladd.fagsakId);
            Objects.requireNonNull(etterkontrollKladd.kontrollType);
            Objects.requireNonNull(etterkontrollKladd.kontrollTidspunkt);
            return etterkontrollKladd;
        }

        public Builder medKontrollType(KontrollType kontrollType) {
            this.etterkontrollKladd.kontrollType = kontrollType;
            return this;
        }

        public Builder medErBehandlet(boolean erBehandlet) {
            this.etterkontrollKladd.erBehandlet = erBehandlet;
            return this;
        }

        public Builder medBehandling(Long behandlingId) {
            this.etterkontrollKladd.behandlingId = behandlingId;
            return this;
        }

        public Builder medKontrollTidspunkt(LocalDateTime kontrollTidspunkt) {
            this.etterkontrollKladd.kontrollTidspunkt = kontrollTidspunkt;
            return this;
        }

    }
}
