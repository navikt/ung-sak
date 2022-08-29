package no.nav.k9.sak.ytelse.beregning.grunnlag;


import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "BeregningsgrunnlagPeriode")
@Table(name = "BG_PERIODE")
@Immutable
public class BeregningsgrunnlagPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_PERIODE")
    private Long id;

    @Column(name = "ekstern_referanse", nullable = false)
    private UUID eksternReferanse;

    @Column(name = "skjaeringstidspunkt", nullable = false)
    private LocalDate skjæringstidspunkt;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public BeregningsgrunnlagPeriode() {
    }

    BeregningsgrunnlagPeriode(BeregningsgrunnlagPeriode grunnlagPeriode) {
        this.skjæringstidspunkt = grunnlagPeriode.skjæringstidspunkt;
        this.eksternReferanse = grunnlagPeriode.eksternReferanse;
    }

    public BeregningsgrunnlagPeriode(UUID eksternReferanse, LocalDate skjæringstidspunkt) {
        this.eksternReferanse = Objects.requireNonNull(eksternReferanse);
        this.skjæringstidspunkt = Objects.requireNonNull(skjæringstidspunkt);
    }

    public Long getId() {
        return id;
    }

    public UUID getEksternReferanse() {
        return eksternReferanse;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BeregningsgrunnlagPeriode that = (BeregningsgrunnlagPeriode) o;
        return Objects.equals(eksternReferanse, that.eksternReferanse) &&
            Objects.equals(skjæringstidspunkt, that.skjæringstidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eksternReferanse, skjæringstidspunkt);
    }

    @Override
    public String toString() {
        return "BeregningsgrunnlagPeriode{" +
            "id=" + id +
            ", eksternReferanse=" + eksternReferanse +
            ", skjæringstidspunkt=" + skjæringstidspunkt +
            '}';
    }
}

