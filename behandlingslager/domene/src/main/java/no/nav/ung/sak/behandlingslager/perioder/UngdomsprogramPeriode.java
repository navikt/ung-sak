package no.nav.ung.sak.behandlingslager.perioder;

import java.time.LocalDate;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;


@Entity(name = "UngdomsprogramPeriode")
@Table(name = "UNG_UNGDOMSPROGRAMPERIODE")
@Immutable
public class UngdomsprogramPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_UNGDOMSPROGRAMPERIODE")
    private Long id;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
            @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    public UngdomsprogramPeriode() {

    }

    public UngdomsprogramPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public UngdomsprogramPeriode(LocalDate fom, LocalDate tom) {
        this(DatoIntervallEntitet.fra(fom, tom));
    }

    public UngdomsprogramPeriode(UngdomsprogramPeriode it) {
        this.periode = it.getPeriode();
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }


}
