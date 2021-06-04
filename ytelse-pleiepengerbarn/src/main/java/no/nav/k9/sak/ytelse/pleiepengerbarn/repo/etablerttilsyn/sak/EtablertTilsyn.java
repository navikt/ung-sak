package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.sak;

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

@Entity(name = "EtablertTilsyn")
@Table(name = "ETABLERT_TILSYN")
public class EtablertTilsyn extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ETABLERT_TILSYN")
    private Long id;

    @Immutable
    @OneToMany(mappedBy = "etablertTilsyn", cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<EtablertTilsynPeriode> perioder = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    EtablertTilsyn() {
        // hibernate
    }
    
    public EtablertTilsyn(List<EtablertTilsynPeriode> perioder) {
        setPerioder(perioder);
    }

    EtablertTilsyn(EtablertTilsyn etablertTilsyn) {
        Objects.requireNonNull(etablertTilsyn);

        this.perioder = etablertTilsyn.getPerioder()
            .stream()
            .map(EtablertTilsynPeriode::new)
            .peek(it -> it.setEtablertTilsyn(this))
            .collect(Collectors.toList());
    }


    public Long getId() {
        return id;
    }

    public List<EtablertTilsynPeriode> getPerioder() {
        return perioder;
    }

    void setPerioder(List<EtablertTilsynPeriode> perioder) {
        this.perioder = perioder.stream()
            .peek(it -> it.setEtablertTilsyn(this))
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "EtablertTilsyn{" +
            "id=" + id +
            ", perioder=" + perioder +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EtablertTilsyn etablertTilsyn = (EtablertTilsyn) o;
        return Objects.equals(perioder, etablertTilsyn.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(perioder);
    }
    

}
