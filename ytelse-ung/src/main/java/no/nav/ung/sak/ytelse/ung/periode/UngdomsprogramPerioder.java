package no.nav.ung.sak.ytelse.ung.periode;

import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;

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
}
