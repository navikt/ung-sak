package no.nav.k9.sak.ytelse.beregning.grunnlag;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import com.google.common.collect.ImmutableList;

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "BeregningsgrunnlagPerioder")
@Table(name = "BG_PERIODER")
@Immutable
class BeregningsgrunnlagPerioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_PERIODER")
    private Long id;

    @Immutable
    @JoinColumn(name = "bg_grunnlag_id", nullable = false, updatable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<BeregningsgrunnlagPeriode> grunnlagPerioder = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    BeregningsgrunnlagPerioder() {
    }

    BeregningsgrunnlagPerioder(BeregningsgrunnlagPerioder grunnlagPeriode) {
        if (grunnlagPeriode != null && grunnlagPeriode.grunnlagPerioder != null) {
            this.grunnlagPerioder = grunnlagPeriode.getGrunnlagPerioder().stream().map(BeregningsgrunnlagPeriode::new).collect(Collectors.toList());
        }
    }

    BeregningsgrunnlagPerioder(List<BeregningsgrunnlagPeriode> grunnlagPerioder) {
        this.grunnlagPerioder = grunnlagPerioder;
    }

    List<BeregningsgrunnlagPeriode> getGrunnlagPerioder() {
        if (grunnlagPerioder == null) {
            return List.of();
        }
        return ImmutableList.copyOf(grunnlagPerioder);
    }

    void deaktiver(LocalDate skjæringstidspunkt) {
        Objects.requireNonNull(skjæringstidspunkt);
        this.grunnlagPerioder.removeIf(it -> it.getSkjæringstidspunkt().equals(skjæringstidspunkt));
    }

    void leggTil(BeregningsgrunnlagPeriode periode) {
        Objects.requireNonNull(periode);
        this.grunnlagPerioder.add(periode);
    }

    @Override
    public String toString() {
        return "BeregningsgrunnlagPeriode{" +
            "id=" + id +
            ", eksternReferanse=" + grunnlagPerioder +
            '}';
    }
}

