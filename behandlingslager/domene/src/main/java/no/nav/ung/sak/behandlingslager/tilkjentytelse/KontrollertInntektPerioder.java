package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.Immutable;

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

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Column(name = "aktiv", nullable = false, updatable = true)
    private boolean aktiv = true;

    @Immutable
    @JoinColumn(name = "kontrollert_inntekt_perioder_id", nullable = false, updatable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<KontrollertInntektPeriode> perioder = new ArrayList<>();


    public Long getBehandlingId() {
        return behandlingId;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void setIkkeAktiv() {
        this.aktiv = false;
    }

    public List<KontrollertInntektPeriode> getPerioder() {
        return perioder;
    }

    public static Builder ny(Long behandlingId) {
        if (behandlingId == null) {
            throw new IllegalArgumentException("behandlingId kan ikke være null");
        }
        return new Builder(behandlingId);
    }

    public static class Builder {
        private Long behandlingId;
        private List<KontrollertInntektPeriode> perioder = new ArrayList<>();
        private String input;
        private String sporing;


        public Builder(Long behandlingId) {
            this.behandlingId = behandlingId;
        }

        public Builder medPerioder(List<KontrollertInntektPeriode> perioder) {
            if (perioder == null) {
                throw new IllegalArgumentException("perioder kan ikke være null");
            }
            this.perioder.addAll(perioder);
            return this;
        }


        public KontrollertInntektPerioder build() {
            KontrollertInntektPerioder tilkjentYtelse = new KontrollertInntektPerioder();
            tilkjentYtelse.behandlingId = this.behandlingId;
            tilkjentYtelse.perioder = this.perioder;
            return tilkjentYtelse;
        }

    }


}
