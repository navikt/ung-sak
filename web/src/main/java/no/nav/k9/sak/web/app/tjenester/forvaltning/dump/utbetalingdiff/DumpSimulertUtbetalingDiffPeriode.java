package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.utbetalingdiff;

import java.util.List;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "DumpSimulertUtbetalingDiffPeriode")
@Table(name = "DUMP_SIMULERT_UTB_DIFF_PERIODE")
public class DumpSimulertUtbetalingDiffPeriode {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_DUMP_SIMULERT_UTB_DIFF_PERIODE")
    private Long id;

    @Embedded
    private DatoIntervallEntitet periode;

    @JoinColumn(name = "periode_id", nullable = false, updatable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<DumpSimulertUtbetalingDiffAndel> andeldiffliste;

    @Column(name = "total_feilutbetaling_bruker", nullable = false)
    private int totalFeilutbetalingBruker;

    @Column(name = "total_feilutbetaling_arbeidsgiver", nullable = false)
    private int totalFeilutbetalingArbeidsgiver;


    public DumpSimulertUtbetalingDiffPeriode() {
    }

    public DumpSimulertUtbetalingDiffPeriode(DatoIntervallEntitet periode, List<DumpSimulertUtbetalingDiffAndel> andeldiffliste) {
        this.periode = periode;
        this.andeldiffliste = andeldiffliste;
    }

    public DumpSimulertUtbetalingDiffPeriode(DatoIntervallEntitet periode, List<DumpSimulertUtbetalingDiffAndel> andeldiffliste, int totalFeilutbetalingBruker, int totalFeilutbetalingArbeidsgiver) {
        this.periode = periode;
        this.andeldiffliste = andeldiffliste;
        this.totalFeilutbetalingBruker = totalFeilutbetalingBruker;
        this.totalFeilutbetalingArbeidsgiver = totalFeilutbetalingArbeidsgiver;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public int getTotalFeilutbetalingBruker() {
        return totalFeilutbetalingBruker;
    }

    public int getTotalFeilutbetalingArbeidsgiver() {
        return totalFeilutbetalingArbeidsgiver;
    }
}
