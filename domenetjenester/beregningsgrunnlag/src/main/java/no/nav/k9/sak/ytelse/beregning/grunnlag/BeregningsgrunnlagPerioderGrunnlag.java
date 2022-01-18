package no.nav.k9.sak.ytelse.beregning.grunnlag;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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

    @ChangeTracked
    @ManyToOne
    @JoinColumn(name = "bg_ovst_input_id", nullable = false, updatable = false)
    private InputOverstyringPerioder inputOverstyringPerioder;

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
        this.inputOverstyringPerioder = eksisterende.inputOverstyringPerioder != null ? new InputOverstyringPerioder(eksisterende.inputOverstyringPerioder) : null;
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

    InputOverstyringPerioder getInputOverstyringHolder() {
        return inputOverstyringPerioder;
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

    public List<InputOverstyringPeriode> getInputOverstyringPerioder() {
        if (inputOverstyringPerioder == null) {
            return List.of();
        }
        return inputOverstyringPerioder.getInputOverstyringPerioder();
    }

    public Optional<BeregningsgrunnlagPeriode> finnGrunnlagFor(LocalDate skjæringstidspunkt) {
        return getGrunnlagPerioder().stream().filter(it -> it.getSkjæringstidspunkt().equals(skjæringstidspunkt)).findFirst();
    }

    public Optional<BeregningsgrunnlagPeriode> finnGrunnlagFor(UUID eksternRef) {
        return getGrunnlagPerioder().stream().filter(it -> it.getEksternReferanse().equals(eksternRef)).findFirst();
    }

    void deaktiver(LocalDate skjæringstidspunkt) {
        Objects.requireNonNull(skjæringstidspunkt);
        deaktiverGrunnlag(skjæringstidspunkt);
    }

    private void deaktiverGrunnlag(LocalDate skjæringstidspunkt) {
        Objects.requireNonNull(skjæringstidspunkt);
        if (this.grunnlagPerioder != null) {
            this.grunnlagPerioder.deaktiver(skjæringstidspunkt);
        }
    }

    void deaktiverKompletthet(LocalDate skjæringstidspunkt) {
        Objects.requireNonNull(skjæringstidspunkt);
        if (this.kompletthetPerioder != null) {
            this.kompletthetPerioder.deaktiver(skjæringstidspunkt);
        }
    }

    void deaktiverInputOverstyring(LocalDate skjæringstidspunkt) {
        Objects.requireNonNull(skjæringstidspunkt);
        if (this.inputOverstyringPerioder != null) {
            this.inputOverstyringPerioder.deaktiver(skjæringstidspunkt);
        }
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

    void leggTil(InputOverstyringPeriode periode) {
        Objects.requireNonNull(periode);
        if (this.inputOverstyringPerioder == null) {
            this.inputOverstyringPerioder = new InputOverstyringPerioder();
        }
        this.inputOverstyringPerioder.leggTil(periode);
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
