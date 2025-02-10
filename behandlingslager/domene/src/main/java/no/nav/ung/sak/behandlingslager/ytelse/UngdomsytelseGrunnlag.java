package no.nav.ung.sak.behandlingslager.ytelse;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsPerioder;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttak;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;


@Entity(name = "UngdomsytelseGrunnlag")
@Table(name = "UNG_GR")
@DynamicInsert
@DynamicUpdate
public class UngdomsytelseGrunnlag {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_GR")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "ung_sats_perioder_id", nullable = false, updatable = false)
    private UngdomsytelseSatsPerioder satsPerioder;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "ung_uttak_perioder_id", updatable = false)
    private UngdomsytelseUttakPerioder uttakPerioder;

    @Column(name = "aktiv", nullable = false, updatable = true)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;


    public UngdomsytelseGrunnlag() {
    }

    public UngdomsytelseSatsPerioder getSatsPerioder() {
        return satsPerioder;
    }

    public LocalDateTimeline<UngdomsytelseSatser> getSatsTidslinje() {
        var segmenter = satsPerioder.getPerioder().stream().map(p -> new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(),
            new UngdomsytelseSatser(p.getDagsats(), p.getGrunnbeløp(), p.getGrunnbeløpFaktor(), p.getSatsType(), p.getAntallBarn(), p.getDagsatsBarnetillegg()))).toList();
        return new LocalDateTimeline<>(segmenter);
    }

    public LocalDateTimeline<UngdomsytelseUttak> getUtbetalingsgradTidslinje() {
        var segmenter = uttakPerioder.getPerioder().stream().map(p ->
            new LocalDateSegment<>(p.getPeriode().getFomDato(), p.getPeriode().getTomDato(), new UngdomsytelseUttak(p.getAvslagsårsak())
            )).toList();
        return new LocalDateTimeline<>(segmenter);
    }

    public UngdomsytelseUttakPerioder getUttakPerioder() {
        return uttakPerioder;
    }

    void setUttakPerioder(UngdomsytelseUttakPerioder uttakPerioder) {
        this.uttakPerioder = uttakPerioder;
    }

    public void setSatsPerioder(UngdomsytelseSatsPerioder satsPerioder) {
        this.satsPerioder = satsPerioder;
    }

    void setIkkeAktivt() {
        this.aktiv = false;
    }

    void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }
}
