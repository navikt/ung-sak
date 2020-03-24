package no.nav.k9.sak.domene.repo.pleiebehov;

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

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "Pleieperioder")
@Table(name = "PB_PLEIEPERIODER")
public class Pleieperioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PB_PLEIEPERIODER")
    private Long id;

    @OneToMany(mappedBy = "pleieperioder", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<Pleieperiode> perioder = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Pleieperioder() {
        // hibernate
    }

    Pleieperioder(Pleieperioder pleieperioder) {
        Objects.requireNonNull(pleieperioder);

        this.perioder = pleieperioder.getPerioder()
            .stream()
            .map(Pleieperiode::new)
            .peek(it -> it.setPleieperioder(this))
            .collect(Collectors.toList());
    }

    public Long getId() {
        return id;
    }

    public List<Pleieperiode> getPerioder() {
        return perioder;
    }

    void setPerioder(List<Pleieperiode> perioder) {
        this.perioder = perioder.stream()
            .peek(it -> it.setPleieperioder(this))
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "Pleieperioder{" +
            "id=" + id +
            ", perioder=" + perioder +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pleieperioder pleieperioder = (Pleieperioder) o;
        return Objects.equals(perioder, pleieperioder.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perioder);
    }
}
