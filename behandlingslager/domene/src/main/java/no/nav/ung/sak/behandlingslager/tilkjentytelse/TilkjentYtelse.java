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


    @Immutable
    @JoinColumn(name = "tilkjent_ytelse_id", nullable = true, updatable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<KorrigertYtelsePeriode> korrigertePerioder = new ArrayList<>();

    @Column(name = "regel_input", nullable = false, updatable = false)
    private String input;

    @Column(name = "regel_sporing", nullable = false, updatable = false)
    private String sporing;


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

    public String getInput() {
        return input;
    }

    public String getSporing() {
        return sporing;
    }

    public static Builder ny(Long behandlingId) {
        if (behandlingId == null) {
            throw new IllegalArgumentException("behandlingId kan ikke være null");
        }
        return new Builder(behandlingId);
    }

    public static class Builder {
        private Long behandlingId;
        private List<TilkjentYtelsePeriode> perioder = new ArrayList<>();
        private List<KorrigertYtelsePeriode> korrigertePerioder = new ArrayList<>();

        private String input;
        private String sporing;


        public Builder(Long behandlingId) {
            this.behandlingId = behandlingId;
        }

        public Builder medPerioder(List<TilkjentYtelsePeriode> perioder) {
            if (perioder == null) {
                throw new IllegalArgumentException("perioder kan ikke være null");
            }
            this.perioder.addAll(perioder);
            return this;
        }

        public Builder medKorrigertePerioder(List<KorrigertYtelsePeriode> perioder) {
            if (perioder == null) {
                throw new IllegalArgumentException("perioder kan ikke være null");
            }
            this.korrigertePerioder.addAll(perioder);
            return this;
        }

        public Builder medInput(String input) {
            if (input == null) {
                throw new IllegalArgumentException("input kan ikke være null");
            }
            this.input = input;
            return this;
        }

        public Builder medSporing(String sporing) {
            if (sporing == null) {
                throw new IllegalArgumentException("sporing kan ikke være null");
            }
            this.sporing = sporing;
            return this;
        }


        public TilkjentYtelse build() {
            TilkjentYtelse tilkjentYtelse = new TilkjentYtelse();
            tilkjentYtelse.behandlingId = this.behandlingId;
            tilkjentYtelse.perioder = this.perioder;
            tilkjentYtelse.korrigertePerioder = this.korrigertePerioder;
            tilkjentYtelse.input = this.input;
            tilkjentYtelse.sporing = this.sporing;
            return tilkjentYtelse;
        }

    }


}
