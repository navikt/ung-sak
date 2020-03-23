package no.nav.k9.sak.behandlingslager.behandling.medisinsk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

@Entity(name = "KontinuerligTilsyn")
@Table(name = "MD_KONTINUERLIG_TILSYN")
public class KontinuerligTilsyn extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MD_KONTINUERLIG_TILSYN")
    private Long id;

    @Immutable
    @OneToMany(mappedBy = "kontinuerligTilsyn", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<KontinuerligTilsynPeriode> perioder = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    KontinuerligTilsyn() {
        // hibernate
    }

    KontinuerligTilsyn(KontinuerligTilsyn kontinuerligTilsyn) {
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

    void setPerioder(List<KontinuerligTilsynPeriode> perioder) {
        this.perioder = perioder.stream()
            .peek(it -> it.setKontinuerligTilsyn(this))
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "KontinuerligTilsyn{" +
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
