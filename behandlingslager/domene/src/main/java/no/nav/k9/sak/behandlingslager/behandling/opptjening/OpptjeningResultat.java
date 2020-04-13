package no.nav.k9.sak.behandlingslager.behandling.opptjening;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "OpptjeningResultat")
@Table(name = "RS_OPPTJENING")
@DynamicInsert
@DynamicUpdate
public class OpptjeningResultat extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RS_OPPTJENING")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    /* Mapper kun fra denne og ikke bi-directional, gjør vedlikehold enklere. */
    @Immutable
    @OneToMany(cascade = {CascadeType.ALL} /* ok siden aktiviteter er eid av denne */, mappedBy = "opptjeningResultat")
    private List<Opptjening> opptjeningPerioder = new ArrayList<>();

    @Column(name = "aktiv", nullable = false, updatable = true)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    /**
     * copy-constructor.
     */
    public OpptjeningResultat(OpptjeningResultat annen) {
        this.opptjeningPerioder.addAll(annen.getOpptjeningPerioder()
            .stream()
            .map(Opptjening::new)
            .peek(it -> it.setOpptjeningResultat(this))
            .collect(Collectors.toList()));
        // kopierer ikke data som ikke er relevante (aktiv, versjon, id, etc)

    }

    OpptjeningResultat() {
        // for hibernate
    }

    public Boolean getAktiv() {
        return aktiv;
    }

    public Long getId() {
        return id;
    }

    public List<Opptjening> getOpptjeningPerioder() {
        // alle returnerte data fra denne klassen skal være immutable
        return Collections.unmodifiableList(opptjeningPerioder);
    }

    void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    void setInaktiv() {
        this.aktiv = false;
        // else - can never go back
    }

    void leggTil(Opptjening opptjening) {
        if (getId() != null) {
            throw new IllegalStateException("Kan ikke modifisere på allerede persistert entitet.");
        }
        this.opptjeningPerioder.add(opptjening);
        opptjening.setOpptjeningResultat(this);
    }

    void deaktiver(LocalDate skjæringstidspunkt) {
        if (getId() != null) {
            throw new IllegalStateException("Kan ikke modifisere på allerede persistert entitet.");
        }
        this.opptjeningPerioder.removeIf(it -> it.getOpptjeningPeriode().getTomDato().plusDays(1).equals(skjæringstidspunkt));
    }

    void deaktiver(DatoIntervallEntitet periode) {
        if (getId() != null) {
            throw new IllegalStateException("Kan ikke modifisere på allerede persistert entitet.");
        }
        this.opptjeningPerioder.removeIf(it -> it.getOpptjeningPeriode().equals(periode));
    }

    public Optional<Opptjening> finnOpptjening(DatoIntervallEntitet periode) {
        return opptjeningPerioder.stream().filter(it -> it.getOpptjeningPeriode().equals(periode)).findFirst();
    }

    public Optional<Opptjening> finnOpptjening(LocalDate skjæringstidspunkt) {
        return opptjeningPerioder.stream().filter(it -> it.getOpptjeningPeriode().getTomDato().plusDays(1).equals(skjæringstidspunkt)).findFirst();
    }

    @Override
    public String toString() {
        return "OpptjeningResultat{" +
            "id=" + id +
            ", behandlingId=" + behandlingId +
            ", opptjeningPerioder=" + opptjeningPerioder +
            ", aktiv=" + aktiv +
            '}';
    }
}
