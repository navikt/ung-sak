package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.omsorg;

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

import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "OmsorgenFor")
@Table(name = "OMSORGEN_FOR")
public class OmsorgenFor extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OMSORGEN_FOR")
    private Long id;

    @Immutable
    @OneToMany(mappedBy = "omsorgenFor", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<OmsorgenForPeriode> perioder = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    OmsorgenFor() {
        // hibernate
    }

    OmsorgenFor(OmsorgenFor omsorgenFor) {
        Objects.requireNonNull(omsorgenFor);

        this.perioder = omsorgenFor.getPerioder()
            .stream()
            .map(OmsorgenForPeriode::new)
            .peek(it -> it.setOmsorgenFor(this))
            .collect(Collectors.toList());
    }


    public Long getId() {
        return id;
    }

    public List<OmsorgenForPeriode> getPerioder() {
        return perioder;
    }

    void setPerioder(List<OmsorgenForPeriode> perioder) {
        this.perioder = perioder.stream()
            .peek(it -> it.setOmsorgenFor(this))
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "OmsorgenFor{" +
            "id=" + id +
            ", perioder=" + perioder +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OmsorgenFor omsorgenFor = (OmsorgenFor) o;
        return Objects.equals(perioder, omsorgenFor.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perioder);
    }
    

}
