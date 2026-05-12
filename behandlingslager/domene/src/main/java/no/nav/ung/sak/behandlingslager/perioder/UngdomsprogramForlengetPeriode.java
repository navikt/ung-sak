package no.nav.ung.sak.behandlingslager.perioder;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.sak.behandlingslager.kodeverk.HjemmelKodeverdiConverter;
import org.hibernate.annotations.Immutable;

/**
 * Angir om bruker har forlenget periode i ungdomsprogrammet (300 virkedager i stedet for 260).
 */
@Entity(name = "UngdomsprogramForlengetPeriode")
@Table(name = "UNG_UNGDOMSPROGRAM_FORLENGET_PERIODE")
@Immutable
public class UngdomsprogramForlengetPeriode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_UNGDOMSPROGRAM_FORLENGET_PERIODE_ID")
    private Long id;

    @Column(name = "har_forlenget_periode", nullable = false)
    private boolean harForlengetPeriode;

    @Convert(converter = HjemmelKodeverdiConverter.class)
    @Column(name = "hjemmel")
    private Hjemmel hjemmel = Hjemmel.UNG_FORSKRIFT_PARAGRAF_6;

    public UngdomsprogramForlengetPeriode() {
    }

    public UngdomsprogramForlengetPeriode(boolean harForlengetPeriode) {
        this.harForlengetPeriode = harForlengetPeriode;
    }

    public UngdomsprogramForlengetPeriode(boolean harForlengetPeriode, Hjemmel hjemmel) {
        this.harForlengetPeriode = harForlengetPeriode;
        this.hjemmel = hjemmel;
    }

    public Long getId() {
        return id;
    }

    public boolean harForlengetPeriode() {
        return harForlengetPeriode;
    }

    public Hjemmel getHjemmel() {
        return hjemmel;
    }
}
