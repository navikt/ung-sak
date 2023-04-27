package no.nav.k9.sak.domene.uttak.repo;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "UttaksgradPerioder")
@Table(name = "UTTAKSGRAD_PERIODER")
public class UttaksgradPerioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UTTAKSGRAD_PERIODER")
    private Long id;

    @Immutable
    @JoinColumn(name = "", nullable = false, updatable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<UttaksgradPeriode> uttaksgradPerioder = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    UttaksgradPerioder() {
    }

    UttaksgradPerioder(UttaksgradPerioder perioder) {

    }

}
