package no.nav.k9.sak.ytelse.beregning.grunnlag;


import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "NæringsinntektPeriode")
@Table(name = "BG_NAERING_INNTEKT_PERIODE")
@Immutable
public class NæringsinntektPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_NAERING_INNTEKT_PERIODE")
    private Long id;

    @Column(name = "iay_referanse", nullable = false)
    private UUID iayReferanse;

    @Column(name = "skjaeringstidspunkt", nullable = false)
    private LocalDate skjæringstidspunkt;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public NæringsinntektPeriode() {
    }

    NæringsinntektPeriode(NæringsinntektPeriode grunnlagPeriode) {
        this.skjæringstidspunkt = grunnlagPeriode.skjæringstidspunkt;
        this.iayReferanse = grunnlagPeriode.iayReferanse;
    }

    public NæringsinntektPeriode(UUID iayReferanse, LocalDate skjæringstidspunkt) {
        this.iayReferanse = Objects.requireNonNull(iayReferanse);
        this.skjæringstidspunkt = Objects.requireNonNull(skjæringstidspunkt);
    }

    public Long getId() {
        return id;
    }

    public UUID getIayReferanse() {
        return iayReferanse;
    }


    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NæringsinntektPeriode that = (NæringsinntektPeriode) o;
        return Objects.equals(iayReferanse, that.iayReferanse) &&
            Objects.equals(skjæringstidspunkt, that.skjæringstidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iayReferanse, skjæringstidspunkt);
    }

    @Override
    public String toString() {
        return "BeregningsgrunnlagPeriode{" +
            "id=" + id +
            ", iayReferanse=" + iayReferanse +
            ", skjæringstidspunkt=" + skjæringstidspunkt +
            '}';
    }
}

