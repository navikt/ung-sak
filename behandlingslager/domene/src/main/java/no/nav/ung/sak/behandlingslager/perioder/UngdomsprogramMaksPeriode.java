package no.nav.ung.sak.behandlingslager.perioder;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.sak.behandlingslager.kodeverk.HjemmelKodeverdiConverter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.util.Optional;

@Entity(name = "UngdomsprogramMaksPeriode")
@Table(name = "UNG_UNGDOMSPROGRAM_MAKS_PERIODE")
@Immutable
public class UngdomsprogramMaksPeriode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_UNGDOMSPROGRAM_MAKS_PERIODE_ID")
    private Long id;

    @Column(name = "har_forlenget_periode", nullable = false)
    private boolean harForlengetPeriode;

    @Column(name = "periode_maks_dato", nullable = false)
    private LocalDate periodeMaksDato;

    @Convert(converter = HjemmelKodeverdiConverter.class)
    @Column(name = "hjemmel")
    private Hjemmel hjemmel = Hjemmel.UNG_FORSKRIFT_PARAGRAF_6;

    public UngdomsprogramMaksPeriode() {
    }

    public UngdomsprogramMaksPeriode(boolean harForlengetPeriode) {
        this.harForlengetPeriode = harForlengetPeriode;
    }

    public UngdomsprogramMaksPeriode(boolean harForlengetPeriode, LocalDate periodeMaksDato) {
        this.harForlengetPeriode = harForlengetPeriode;
        this.periodeMaksDato = periodeMaksDato;
    }

    public UngdomsprogramMaksPeriode(boolean harForlengetPeriode, LocalDate periodeMaksDato, Hjemmel hjemmel) {
        this.harForlengetPeriode = harForlengetPeriode;
        this.periodeMaksDato = periodeMaksDato;
        this.hjemmel = hjemmel;
    }

    public UngdomsprogramMaksPeriode(boolean harForlengetPeriode, Hjemmel hjemmel) {
        this.harForlengetPeriode = harForlengetPeriode;
        this.hjemmel = hjemmel;
    }

    public Long getId() {
        return id;
    }

    public boolean harForlengetPeriode() {
        return harForlengetPeriode;
    }

    public Optional<LocalDate> getPeriodeMaksDato() {
        return Optional.ofNullable(periodeMaksDato);
    }

    public Hjemmel getHjemmel() {
        return hjemmel;
    }
}
