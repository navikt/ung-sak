package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.utbetalingdiff;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "DataDumpGrunnlag")
@Table(name = "DUMP_SIMULERT_UTB")
public class DataDumpSimulertUtbetaling extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_DUMP_SIMULERT_UTB")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @OneToMany
    @JoinColumn(name = "dump_grunnlag_id", nullable = true, updatable = false)
    private List<DumpSimulertUtbetalingDiff> simulertUtbetalingListe;

    public DataDumpSimulertUtbetaling() {
    }

    public DataDumpSimulertUtbetaling(Long behandlingId,
                                      List<DumpSimulertUtbetalingDiff> simulertUtbetalingListe) {
        this.behandlingId = behandlingId;
        this.simulertUtbetalingListe = simulertUtbetalingListe;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public List<DumpSimulertUtbetalingDiff> getSimulertUtbetalingListe() {
        return simulertUtbetalingListe;
    }
}
