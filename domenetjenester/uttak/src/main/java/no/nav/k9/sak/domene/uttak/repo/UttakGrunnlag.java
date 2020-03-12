package no.nav.k9.sak.domene.uttak.repo;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity(name = "UttakGrunnlag")
@Table(name = "GR_UTTAK")
class UttakGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_UTTAK")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ManyToOne
    @Immutable
    @JoinColumn(name = "oppgitt_uttak_id", nullable = false, updatable = false, unique = true)
    private Uttak oppgittUttak;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UttakGrunnlag() {
    }

    UttakGrunnlag(Long behandlingId, Uttak uttak) {
        this.behandlingId = behandlingId;
        this.oppgittUttak = uttak; // NOSONAR
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public Uttak getOppgittFordeling() {
        return oppgittUttak;
    }

    void setOppgittFordeling(Uttak oppgittFordeling) {
        this.oppgittUttak = oppgittFordeling;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof UttakGrunnlag)) return false;
        var that = this.getClass().cast(o);
        return Objects.equals(oppgittUttak, that.oppgittUttak);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgittUttak);
    }

    @Override
    public String toString() {
        return "UttakGrunnlag{" +
            "id=" + id +
            ", behandling=" + behandlingId +
            ", oppgittUttak=" + oppgittUttak +
            ", aktiv=" + aktiv +
            ", versjon=" + versjon +
            '}';
    }
}
