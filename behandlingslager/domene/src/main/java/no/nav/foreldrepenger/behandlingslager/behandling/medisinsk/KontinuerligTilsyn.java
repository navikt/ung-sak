package no.nav.foreldrepenger.behandlingslager.behandling.medisinsk;

import java.util.List;
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

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

@Entity(name = "KontinuerligTilsyn")
@Table(name = "MD_KONTINUERLIG_TILSYN")
public class KontinuerligTilsyn extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MD_KONTINUERLIG_TILSYN")
    private Long id;

    @OneToMany(mappedBy = "kontinuerligTilsyn", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<KontinuerligTilsynPeriode> perioder;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    KontinuerligTilsyn() {
        // hibernate
    }

    public KontinuerligTilsyn(Set<KontinuerligTilsynPeriode> perioder) {
        Objects.requireNonNull(perioder);
        this.perioder = perioder.stream()
            .peek(it -> it.setKontinuerligTilsyn(this))
            .collect(Collectors.toList());
    }

    public KontinuerligTilsyn(KontinuerligTilsyn kontinuerligTilsyn) {
        Objects.requireNonNull(kontinuerligTilsyn);

        this.perioder = kontinuerligTilsyn.getPerioder()
            .stream()
            .map(KontinuerligTilsynPeriode::new)
            .peek(it -> it.setKontinuerligTilsyn(this))
            .collect(Collectors.toList());
    }

    public Long getId() {
        return id;
    }

    public List<KontinuerligTilsynPeriode> getPerioder() {
        return perioder;
    }

    @Override
    public String toString() {
        return "Fordeling{" +
            "id=" + id +
            ", perioder=" + perioder +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KontinuerligTilsyn kontinuerligTilsyn = (KontinuerligTilsyn) o;
        return Objects.equals(perioder, kontinuerligTilsyn.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perioder);
    }
}
