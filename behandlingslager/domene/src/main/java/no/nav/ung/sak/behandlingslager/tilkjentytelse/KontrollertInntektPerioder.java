package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.persistence.*;
import no.nav.ung.sak.felles.BaseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.sporing.RegelData;
import no.nav.ung.sak.felles.diff.DiffIgnore;
import org.hibernate.annotations.Immutable;

import java.sql.Clob;
import java.util.ArrayList;
import java.util.List;

/**
 * Holder på perioder der det er utført kontroll av inntekt mot opplysninger i a-ordningen.
 */
@Entity(name = "KontrollertInntektPerioder")
@Table(name = "KONTROLLERT_INNTEKT_PERIODER")
public class KontrollertInntektPerioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_KONTROLLERT_INNTEKT_PERIODER")
    private Long id;

    @Lob
    @Column(name = "regel_input")
    @DiffIgnore
    private Clob regelInput; // Ved helmanuell kontroll av inntekt vil denne være null

    @Lob
    @Column(name = "regel_sporing")
    @DiffIgnore
    private Clob regelSporing; // Ved helmanuell kontroll av inntekt vil denne være null

    @Immutable
    @JoinColumn(name = "kontrollert_inntekt_perioder_id", nullable = false, updatable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<KontrollertInntektPeriode> perioder = new ArrayList<>();

    public KontrollertInntektPerioder() {
    }

    public List<KontrollertInntektPeriode> getPerioder() {
        return perioder;
    }

    public RegelData getRegelInput() {
        return regelInput == null ? null : new RegelData(regelInput);
    }

    public RegelData getRegelSporing() {
        return regelSporing == null ? null : new RegelData(regelSporing);
    }


    public static Builder ny(Long behandlingId) {
        if (behandlingId == null) {
            throw new IllegalArgumentException("behandlingId kan ikke være null");
        }
        return new Builder();
    }

    public static Builder kopi(KontrollertInntektPerioder perioder) {
        return new Builder(perioder);
    }

    public static class Builder {
        private List<KontrollertInntektPeriode> perioder = new ArrayList<>();
        private Clob regelInput;
        private Clob regelSporing;


        public Builder(KontrollertInntektPerioder perioder) {

            this.perioder = perioder.perioder.stream().map(KontrollertInntektPeriode::new).toList();
        }

        public Builder() {
        }

        public Builder medPerioder(List<KontrollertInntektPeriode> perioder) {
            if (perioder == null) {
                throw new IllegalArgumentException("perioder kan ikke være null");
            }
            this.perioder.addAll(perioder);
            return this;
        }

        public Builder medRegelInput(String data) {
            this.regelInput = data == null ? null : new RegelData(data).getClob();
            return this;
        }

        public Builder medRegelSporing(String data) {
            this.regelSporing = data == null ? null : new RegelData(data).getClob();
            return this;
        }


        public KontrollertInntektPerioder build() {
            KontrollertInntektPerioder kontrollertInntektPerioder = new KontrollertInntektPerioder();
            kontrollertInntektPerioder.perioder = this.perioder;
            kontrollertInntektPerioder.regelInput = this.regelInput;
            kontrollertInntektPerioder.regelSporing = this.regelSporing;
            return kontrollertInntektPerioder;
        }

    }


}
