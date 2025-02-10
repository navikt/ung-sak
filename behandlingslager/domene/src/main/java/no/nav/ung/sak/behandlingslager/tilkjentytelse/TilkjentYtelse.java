package no.nav.ung.sak.behandlingslager.tilkjentytelse;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.Immutable;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "TilkjentYtelse")
@Table(name = "TILKJENT_YTELSE")
public class TilkjentYtelse extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_TILKJENT_YTELSE")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Column(name = "aktiv", nullable = false, updatable = true)
    private boolean aktiv = true;


    @Immutable
    @JoinColumn(name = "tilkjent_ytelse_id", nullable = false, updatable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<TilkjentYtelsePeriode> perioder = new ArrayList<>();


    public Long getBehandlingId() {
        return behandlingId;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void setIkkeAktiv() {
        this.aktiv = false;
    }

    public List<TilkjentYtelsePeriode> getPerioder() {
        return perioder;
    }

    public static Builder ny(Long behandlingId) {
        return new Builder(behandlingId);
    }

    public static class Builder {
        private Long behandlingId;
        private List<TilkjentYtelsePeriode> perioder = new ArrayList<>();

        public Builder(Long behandlingId) {
            this.behandlingId = behandlingId;
        }

        public Builder medPerioder(List<TilkjentYtelsePeriode> perioder) {
            this.perioder.addAll(perioder);
            return this;
        }

        public TilkjentYtelse build() {
            TilkjentYtelse tilkjentYtelse = new TilkjentYtelse();
            tilkjentYtelse.behandlingId = this.behandlingId;
            tilkjentYtelse.perioder = this.perioder;
            return tilkjentYtelse;
        }

    }


}
