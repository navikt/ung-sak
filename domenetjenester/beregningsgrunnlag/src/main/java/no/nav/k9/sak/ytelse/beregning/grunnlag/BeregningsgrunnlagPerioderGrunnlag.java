package no.nav.k9.sak.ytelse.beregning.grunnlag;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "BeregningsgrunnlagPerioderGrunnlag")
@Table(name = "GR_BEREGNINGSGRUNNLAG")
@DynamicInsert
@DynamicUpdate
public class BeregningsgrunnlagPerioderGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_BEREGNINGSGRUNNLAG")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "bg_grunnlag_id", nullable = false, updatable = false)
    private BeregningsgrunnlagPerioder grunnlagPerioder;

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "bg_komplett_id", nullable = false, updatable = false)
    private KompletthetPerioder kompletthetPerioder;

    @Column(name = "aktiv", nullable = false, updatable = true)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    BeregningsgrunnlagPerioderGrunnlag() {
    }

    BeregningsgrunnlagPerioderGrunnlag(BeregningsgrunnlagPerioderGrunnlag eksisterende) {
        this.grunnlagPerioder = eksisterende.grunnlagPerioder != null ? new BeregningsgrunnlagPerioder(eksisterende.grunnlagPerioder) : null;
        this.kompletthetPerioder = eksisterende.kompletthetPerioder != null ? new KompletthetPerioder(eksisterende.kompletthetPerioder) : null;
    }

    void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    BeregningsgrunnlagPerioder getGrunnlagHolder() {
        return grunnlagPerioder;
    }

    KompletthetPerioder getKompletthetHolder() {
        return kompletthetPerioder;
    }

    public List<BeregningsgrunnlagPeriode> getGrunnlagPerioder() {
        if (grunnlagPerioder == null) {
            return List.of();
        }
        return grunnlagPerioder.getGrunnlagPerioder();
    }

    public List<KompletthetPeriode> getKompletthetPerioder() {
        if (kompletthetPerioder == null) {
            return List.of();
        }
        return kompletthetPerioder.getKompletthetPerioder();
    }

    public Optional<BeregningsgrunnlagPeriode> finnGrunnlagFor(LocalDate skjæringstidspunkt) {
        return getGrunnlagPerioder().stream().filter(it -> it.getSkjæringstidspunkt().equals(skjæringstidspunkt)).findFirst();
    }

    public Optional<BeregningsgrunnlagPeriode> finnGrunnlagFor(UUID eksternRef) {
        return getGrunnlagPerioder().stream().filter(it -> it.getEksternReferanse().equals(eksternRef)).findFirst();
    }

    void deaktiver(LocalDate skjæringstidspunkt) {
        Objects.requireNonNull(skjæringstidspunkt);
        if (this.grunnlagPerioder == null) {
            this.grunnlagPerioder = new BeregningsgrunnlagPerioder();
        }
        this.grunnlagPerioder.deaktiver(skjæringstidspunkt);
    }

    void leggTil(BeregningsgrunnlagPeriode periode) {
        Objects.requireNonNull(periode);
        if (this.grunnlagPerioder == null) {
            this.grunnlagPerioder = new BeregningsgrunnlagPerioder();
        }
        this.grunnlagPerioder.leggTil(periode);
    }

    void leggTil(KompletthetPeriode periode) {
        Objects.requireNonNull(periode);
        if (this.kompletthetPerioder == null) {
            this.kompletthetPerioder = new KompletthetPerioder();
        }
        this.kompletthetPerioder.leggTil(periode);
    }

    void setIkkeAktivt() {
        this.aktiv = false;
    }

    @Override
    public String toString() {
        return "BeregningsgrunnlagPerioderGrunnlag{" +
            "id=" + id +
            ", grunnlagPerioder=" + grunnlagPerioder +
            ", kompletthetPerioder=" + kompletthetPerioder +
            '}';
    }
}
