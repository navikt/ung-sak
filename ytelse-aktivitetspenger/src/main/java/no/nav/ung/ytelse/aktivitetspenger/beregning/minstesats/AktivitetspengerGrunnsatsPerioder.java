package no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.sporing.RegelData;
import no.nav.ung.sak.diff.DiffIgnore;
import org.hibernate.annotations.Immutable;

import java.sql.Clob;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "AktivitetspengerGrunnsatsPerioder")
@Table(name = "AVP_GRUNNSATS_PERIODER")
@Immutable
public class AktivitetspengerGrunnsatsPerioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_AVP_GRUNNSATS_PERIODER")
    private Long id;

    @Immutable
    @JoinColumn(name = "avp_grunnsats_perioder_id", nullable = false, updatable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<AktivitetspengerGrunnsatsPeriode> perioder = new ArrayList<>();

    @Lob
    @Column(name = "regel_input", nullable = false)
    @DiffIgnore
    private Clob regelInput;

    @Lob
    @Column(name = "regel_sporing", nullable = false)
    @DiffIgnore
    private Clob regelSporing;

    public AktivitetspengerGrunnsatsPerioder() {
    }

    public AktivitetspengerGrunnsatsPerioder(List<AktivitetspengerGrunnsatsPeriode> perioder, String regelInput, String regelSporing) {
        this.perioder = perioder != null ? perioder.stream().map(AktivitetspengerGrunnsatsPeriode::new).toList() : null;
        this.regelInput = new RegelData(regelInput).getClob();
        this.regelSporing = new RegelData(regelSporing).getClob();
    }

    public List<AktivitetspengerGrunnsatsPeriode> getPerioder() {
        return perioder;
    }

    public RegelData getRegelInput() {
        return regelInput == null ? null : new RegelData(regelInput);
    }

    public RegelData getRegelSporing() {
        return regelSporing == null ? null : new RegelData(regelSporing);
    }

    public void setRegelInput(String data) {
        setRegelInput(data == null ? null : new RegelData(data));
    }

    public void setRegelInput(RegelData data) {
        if (this.regelInput != null) {
            throw new IllegalStateException("regelInput allerede satt, kan ikke sette på nytt: " + data);
        }
        this.regelInput = data == null ? null : data.getClob();
    }

    public void setRegelSporing(String data) {
        setRegelSporing(data == null ? null : new RegelData(data));
    }

    public void setRegelSporing(RegelData data) {
        if (this.regelSporing != null) {
            throw new IllegalStateException("regelSporing allerede satt, kan ikke sette på nytt: " + data);
        }
        this.regelSporing = data == null ? null : data.getClob();
    }
}

