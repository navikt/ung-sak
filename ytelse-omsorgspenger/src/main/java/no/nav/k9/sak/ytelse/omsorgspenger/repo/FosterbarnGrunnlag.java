package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import java.util.Objects;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "FosterbarnGrunnlag")
@Table(name = "OMP_GR_FOSTERBARN")
@DynamicUpdate
public class FosterbarnGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OMP_GR_FOSTERBARN")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "behandling_id", updatable = false, nullable = false)
    private Long behandlingId;


    @Column(name = "aktiv", nullable = false)
    private Boolean aktiv = true;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "omp_fosterbarna_id", updatable = false)
    private Fosterbarna fosterbarna;

    FosterbarnGrunnlag() {
        // Hibernate
    }

    FosterbarnGrunnlag(Long behandlingId, Fosterbarna fosterbarna) {
        this.behandlingId = behandlingId;
        this.fosterbarna = fosterbarna;
    }

    void setAktiv(final boolean aktiv) {
        this.aktiv = aktiv;
    }

    public Fosterbarna getFosterbarna() {
        return fosterbarna;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FosterbarnGrunnlag that = (FosterbarnGrunnlag) o;
        return Objects.equals(behandlingId, that.behandlingId) &&
                Objects.equals(fosterbarna, that.fosterbarna);
    }


    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, fosterbarna);
    }

    @Override
    public String toString() {
        return "FosterbarnGrunnlag{" +
            "id=" + id +
            ", versjon=" + versjon +
            ", behandlingId=" + behandlingId +
            ", aktiv=" + aktiv +
            ", fosterbarna=" + fosterbarna +
            '}';
    }
}
