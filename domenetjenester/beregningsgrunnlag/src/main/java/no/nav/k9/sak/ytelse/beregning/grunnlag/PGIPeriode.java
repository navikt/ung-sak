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

@Entity(name = "PGIPeriode")
@Table(name = "BG_PGI_PERIODE")
@Immutable
public class PGIPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_PGI_PERIODE")
    private Long id;

    @Column(name = "iay_referanse", nullable = false)
    private UUID iayReferanse;

    @Column(name = "skjaeringstidspunkt", nullable = false)
    private LocalDate skjæringstidspunkt;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public PGIPeriode() {
    }

    PGIPeriode(PGIPeriode grunnlagPeriode) {
        this.skjæringstidspunkt = grunnlagPeriode.skjæringstidspunkt;
        this.iayReferanse = grunnlagPeriode.iayReferanse;
    }

    public PGIPeriode(UUID iayReferanse, LocalDate skjæringstidspunkt) {
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
        PGIPeriode that = (PGIPeriode) o;
        return Objects.equals(iayReferanse, that.iayReferanse) &&
            Objects.equals(skjæringstidspunkt, that.skjæringstidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(iayReferanse, skjæringstidspunkt);
    }

    @Override
    public String toString() {
        return "PGIPeriode{" +
            "id=" + id +
            ", iayReferanse=" + iayReferanse +
            ", skjæringstidspunkt=" + skjæringstidspunkt +
            '}';
    }
}

