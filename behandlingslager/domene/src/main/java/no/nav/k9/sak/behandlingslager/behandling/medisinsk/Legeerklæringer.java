package no.nav.k9.sak.behandlingslager.behandling.medisinsk;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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

import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "Legeerklæringer")
@Table(name = "MD_LEGEERKLAERINGER")
public class Legeerklæringer extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MD_LEGEERKLAERINGER")
    private Long id;

    @Immutable
    @OneToMany(mappedBy = "legeerklæringer", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private Set<Legeerklæring> legeerklæringer;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Legeerklæringer() {
        // hibernate
    }

    public Legeerklæringer(Legeerklæringer legeerklæringer) {
        if (legeerklæringer != null) {
            this.legeerklæringer = legeerklæringer.getLegeerklæringer()
                .stream()
                .map(Legeerklæring::new)
                .peek(it -> it.setLegeerklæringer(this))
                .collect(Collectors.toSet());
        } else {
            this.legeerklæringer = new HashSet<>();
        }
    }

    public Legeerklæringer(Set<Legeerklæring> perioder) {
        Objects.requireNonNull(perioder);
        this.legeerklæringer = perioder.stream()
            .peek(it -> it.setLegeerklæringer(this))
            .collect(Collectors.toSet());
    }

    public Long getId() {
        return id;
    }

    public Set<Legeerklæring> getLegeerklæringer() {
        return legeerklæringer;
    }

    public void leggTilLegeerklæring(Legeerklæring legeerklæring) {
        if (id != null) {
            throw new IllegalStateException("Utvikler feil: Kan ikke manipulere persistert parent entitet");
        } else {
            legeerklæringer.removeIf(it -> it.getUuid().equals(legeerklæring.getUuid()));
            legeerklæring.setLegeerklæringer(this);
            legeerklæringer.add(legeerklæring);
        }
    }

    @Override
    public String toString() {
        return "Legeerklæringer{" +
            "id=" + id +
            ", legeerklæringer=" + legeerklæringer +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Legeerklæringer fordeling = (Legeerklæringer) o;
        return Objects.equals(legeerklæringer, fordeling.legeerklæringer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(legeerklæringer);
    }
}
