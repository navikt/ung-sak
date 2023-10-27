package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.utbetalingdiff;

import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "DumpSimulertUtbetalingDiff")
@Table(name = "DUMP_SIMULERT_UTB_DIFF")
public class DumpSimulertUtbetalingDiff extends BaseEntitet {


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_DUMP_SIMULERT_UTB_DIFF")
    private Long id;

    @Column(name = "ekstern_referanse", nullable = false)
    private UUID eksternReferanse;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "kalkulus_request", nullable = false, updatable = false)
    private String kalkulusRequest;

    @OneToMany
    @JoinColumn(name = "dump_simulert_utb_diff_id", nullable = false, updatable = false)
    private List<DumpSimulertUtbetalingDiffPeriode> perioder;

    @Column(name = "total_feilutbetaling_bruker", nullable = false)
    private int totalFeilutbetalingBruker;

    @Column(name = "total_feilutbetaling_arbeidsgiver", nullable = false)
    private int totalFeilutbetalingArbeidsgiver;


    public DumpSimulertUtbetalingDiff() {
    }

    public DumpSimulertUtbetalingDiff(UUID eksternReferanse, String kalkulusRequest, List<DumpSimulertUtbetalingDiffPeriode> perioder) {
        this.eksternReferanse = eksternReferanse;
        this.kalkulusRequest = kalkulusRequest;
        this.perioder = perioder;
    }


    public DumpSimulertUtbetalingDiff(UUID eksternReferanse, String kalkulusRequest, List<DumpSimulertUtbetalingDiffPeriode> perioder, int totalFeilutbetalingBruker, int totalFeilutbetalingArbeidsgiver) {
        this.eksternReferanse = eksternReferanse;
        this.kalkulusRequest = kalkulusRequest;
        this.perioder = perioder;
        this.totalFeilutbetalingBruker = totalFeilutbetalingBruker;
        this.totalFeilutbetalingArbeidsgiver = totalFeilutbetalingArbeidsgiver;
    }

    public UUID getEksternReferanse() {
        return eksternReferanse;
    }

    public String getKalkulusRequest() {
        return kalkulusRequest;
    }

    public List<DumpSimulertUtbetalingDiffPeriode> getPerioder() {
        return perioder;
    }

    public int getTotalFeilutbetalingBruker() {
        return totalFeilutbetalingBruker;
    }

    public int getTotalFeilutbetalingArbeidsgiver() {
        return totalFeilutbetalingArbeidsgiver;
    }
}
