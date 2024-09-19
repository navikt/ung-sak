package no.nav.k9.sak.ytelse.ung.beregning;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

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

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UngdomsytelseSatsPerioder(List<UngdomsytelseSatsPeriode> satsPerioder) {
        this.satsPerioder = satsPerioder.stream().map(UngdomsytelseSatsPeriode::new).toList();
    }

    public UngdomsytelseSatsPerioder(UngdomsytelseSatsPerioder satsPerioder) {
        this(satsPerioder.satsPerioder);
    }


    public UngdomsytelseSatsPerioder() {
    }

    public List<UngdomsytelseSatsPeriode> getSatsPerioder() {
        return satsPerioder;
    }


}
