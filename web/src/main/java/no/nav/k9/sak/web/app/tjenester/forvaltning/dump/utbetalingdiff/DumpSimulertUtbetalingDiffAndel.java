package no.nav.k9.sak.web.app.tjenester.forvaltning.dump.utbetalingdiff;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;

@Entity(name = "DumpSimulertUtbetalingDiffAndel")
@Table(name = "DUMP_SIMULERT_UTB_DIFF_ANDEL")
public class DumpSimulertUtbetalingDiffAndel extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_DUMP_SIMULERT_UTB_DIFF_ANDEL")
    private Long id;

    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @Column(name = "dagsats_aktiv", nullable = false)
    private int dagsatsAktiv;

    @Column(name = "dagsats_simulert", nullable = false)
    private int dagsatsSimulert;

    @Column(name = "dagsats_bruker_aktiv", nullable = false)
    private int dagsatsBrukerAktiv;

    @Column(name = "dagsats_bruker_simulert", nullable = false)
    private int dagsatsBrukerSimulert;

    @Column(name = "dagsats_arbeidsgiver_aktiv", nullable = false)
    private int dagsatsArbeidsgiverAktiv;

    @Column(name = "dagsats_arbeidsgiver_simulert", nullable = false)
    private int dagsatsArbeidsgiverSimulert;

    public DumpSimulertUtbetalingDiffAndel() {
    }

    public DumpSimulertUtbetalingDiffAndel(Arbeidsgiver arbeidsgiver,
                                           int dagsatsAktiv,
                                           int dagsatsSimulert,
                                           int dagsatsBrukerAktiv,
                                           int dagsatsBrukerSimulert,
                                           int dagsatsArbeidsgiverAktiv,
                                           int dagsatsArbeidsgiverSimulert) {
        this.arbeidsgiver = arbeidsgiver;
        this.dagsatsAktiv = dagsatsAktiv;
        this.dagsatsSimulert = dagsatsSimulert;
        this.dagsatsBrukerAktiv = dagsatsBrukerAktiv;
        this.dagsatsBrukerSimulert = dagsatsBrukerSimulert;
        this.dagsatsArbeidsgiverAktiv = dagsatsArbeidsgiverAktiv;
        this.dagsatsArbeidsgiverSimulert = dagsatsArbeidsgiverSimulert;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public int getDagsatsAktiv() {
        return dagsatsAktiv;
    }

    public int getDagsatsSimulert() {
        return dagsatsSimulert;
    }

    public int getDagsatsBrukerAktiv() {
        return dagsatsBrukerAktiv;
    }

    public int getDagsatsBrukerSimulert() {
        return dagsatsBrukerSimulert;
    }

    public int getDagsatsArbeidsgiverAktiv() {
        return dagsatsArbeidsgiverAktiv;
    }

    public int getDagsatsArbeidsgiverSimulert() {
        return dagsatsArbeidsgiverSimulert;
    }
}
