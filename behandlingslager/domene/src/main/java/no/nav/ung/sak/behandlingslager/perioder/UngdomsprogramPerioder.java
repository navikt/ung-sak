package no.nav.ung.sak.behandlingslager.perioder;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import java.util.Objects;
import java.util.Set;

@Entity(name = "UngdomsprogramPerioder")
@Table(name = "UNG_UNGDOMSPROGRAMPERIODER")
@Immutable
public class UngdomsprogramPerioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_UNGDOMSPROGRAMPERIODER")
    private Long id;

    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "ung_ungdomsprogramperioder_id", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH}, orphanRemoval = true)
    private Set<UngdomsprogramPeriode> perioder;


    public UngdomsprogramPerioder() {
    }

    public UngdomsprogramPerioder(Set<UngdomsprogramPeriode> perioder) {
        this.perioder = perioder;
    }


    public Set<UngdomsprogramPeriode> getPerioder() {
        return perioder;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UngdomsprogramPerioder that)) return false;
        return Objects.equals(perioder, that.perioder);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(perioder);
    }
}
