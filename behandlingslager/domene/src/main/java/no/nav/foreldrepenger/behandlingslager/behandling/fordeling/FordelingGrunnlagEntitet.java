package no.nav.foreldrepenger.behandlingslager.behandling.fordeling;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

@Entity(name = "FordelingGrunnlag")
@Table(name = "GR_FORDELING")
class FordelingGrunnlagEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_FORDELING")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @OneToOne
    @Immutable
    @JoinColumn(name = "oppgitt_fordeling_id", nullable = false, updatable = false, unique = true)
    private Fordeling oppgittFordeling;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    FordelingGrunnlagEntitet() {
    }

    FordelingGrunnlagEntitet(Behandling behandling, Fordeling fordeling) {
        this.behandlingId = behandling.getId();
        this.oppgittFordeling = fordeling; // NOSONAR
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    public Fordeling getOppgittFordeling() {
        return oppgittFordeling;
    }

    void setOppgittFordeling(Fordeling oppgittFordeling) {
        this.oppgittFordeling = oppgittFordeling;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FordelingGrunnlagEntitet that = (FordelingGrunnlagEntitet) o;
        return Objects.equals(oppgittFordeling, that.oppgittFordeling);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgittFordeling);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            "id=" + id +
            ", behandling=" + behandlingId +
            ", oppgittFordeling=" + oppgittFordeling +
            ", aktiv=" + aktiv +
            ", versjon=" + versjon +
            '>';
    }
}
