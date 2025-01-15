package no.nav.ung.sak.behandlingslager.ytelse.uttak;

import java.sql.Clob;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.beregning.RegelData;
import no.nav.ung.sak.behandlingslager.diff.DiffIgnore;

@Entity(name = "UngdomsytelseUttakPerioder")
@Table(name = "UNG_UTTAK_PERIODER")
@Immutable
public class UngdomsytelseUttakPerioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_UTTAK_PERIODER")
    private Long id;

    @Immutable
    @JoinColumn(name = "ung_uttak_perioder_id", nullable = false, updatable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<UngdomsytelseUttakPeriode> uttakPerioder = new ArrayList<>();

    @Lob
    @Column(name = "regel_input", nullable = false)
    @DiffIgnore
    private Clob regelInput;

    @Lob
    @Column(name = "regel_sporing", nullable = false)
    @DiffIgnore
    private Clob regelSporing;

    public UngdomsytelseUttakPerioder() {
    }

    public UngdomsytelseUttakPerioder(List<UngdomsytelseUttakPeriode> perioder) {
        this.uttakPerioder = perioder != null ? perioder.stream().map(UngdomsytelseUttakPeriode::new).toList() : null;
    }

    public UngdomsytelseUttakPerioder(UngdomsytelseUttakPerioder perioder) {
        this(perioder.uttakPerioder);
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

    public List<UngdomsytelseUttakPeriode> getPerioder() {
        return uttakPerioder;
    }

}
