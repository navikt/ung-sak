package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleiebehov;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "Pleieperioder")
@Table(name = "PB_PLEIEPERIODER")
public class EtablertPleieperioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PB_PLEIEPERIODER")
    private Long id;

    @OneToMany(mappedBy = "pleieperioder", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<EtablertPleieperiode> perioder = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    EtablertPleieperioder() {
        // hibernate
    }

    EtablertPleieperioder(EtablertPleieperioder pleieperioder) {
        Objects.requireNonNull(pleieperioder);

        this.perioder = pleieperioder.getPerioder()
            .stream()
            .map(it -> new EtablertPleieperiode(it))
            .peek(it -> it.setPleieperioder(this))
            .collect(Collectors.toList());
    }

    public Long getId() {
        return id;
    }

    public List<EtablertPleieperiode> getPerioder() {
        return perioder;
    }

    void setPerioder(List<EtablertPleieperiode> perioder) {
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
        EtablertPleieperioder pleieperioder = (EtablertPleieperioder) o;
        return Objects.equals(perioder, pleieperioder.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perioder);
    }
}
