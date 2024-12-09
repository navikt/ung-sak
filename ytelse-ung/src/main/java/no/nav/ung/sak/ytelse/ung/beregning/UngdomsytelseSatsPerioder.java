package no.nav.ung.sak.ytelse.ung.beregning;

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

@Entity(name = "UngdomsytelseSatsPerioder")
@Table(name = "UNG_SATS_PERIODER")
@Immutable
public class UngdomsytelseSatsPerioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_SATS_PERIODER")
    private Long id;

    @Immutable
    @JoinColumn(name = "ung_sats_perioder_id", nullable = false, updatable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<UngdomsytelseSatsPeriode> satsPerioder = new ArrayList<>();

    @Lob
    @Column(name = "regel_input", nullable = false)
    @DiffIgnore
    private Clob regelInput;

    @Lob
    @Column(name = "regel_sporing", nullable = false)
    @DiffIgnore
    private Clob regelSporing;


    public UngdomsytelseSatsPerioder(List<UngdomsytelseSatsPeriode> satsPerioder, Clob regelInput, Clob regelSporing) {
        this.satsPerioder = satsPerioder != null ? satsPerioder.stream().map(UngdomsytelseSatsPeriode::new).toList() : null;
        this.regelInput = regelInput;
        this.regelSporing = regelSporing;
    }

    public UngdomsytelseSatsPerioder(UngdomsytelseSatsPerioder satsPerioder) {
        this(satsPerioder.satsPerioder, satsPerioder.regelInput, satsPerioder.regelSporing);
    }


    public UngdomsytelseSatsPerioder() {
    }

    public List<UngdomsytelseSatsPeriode> getPerioder() {
        return satsPerioder;
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
