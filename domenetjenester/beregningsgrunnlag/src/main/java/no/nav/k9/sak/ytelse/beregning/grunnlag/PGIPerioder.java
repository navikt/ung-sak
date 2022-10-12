package no.nav.k9.sak.ytelse.beregning.grunnlag;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "PGIPerioder")
@Table(name = "BG_PGI_PERIODER")
@Immutable
class PGIPerioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_PGI_PERIODER")
    private Long id;

    @Immutable
    @JoinColumn(name = "bg_pgi_id", nullable = false, updatable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<PGIPeriode> PGIPerioder = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    PGIPerioder() {
    }

    PGIPerioder(PGIPerioder inntektPerioder) {
        if (inntektPerioder != null && inntektPerioder.PGIPerioder != null) {
            this.PGIPerioder = inntektPerioder.getPGIPerioder().stream().map(PGIPeriode::new).collect(Collectors.toList());
        }
    }

    PGIPerioder(List<PGIPeriode> PGIPerioder) {
        this.PGIPerioder = PGIPerioder;
    }

    List<PGIPeriode> getPGIPerioder() {
        if (PGIPerioder == null) {
            return List.of();
        }
        return List.copyOf(PGIPerioder);
    }

    void deaktiver(LocalDate skjæringstidspunkt) {
        Objects.requireNonNull(skjæringstidspunkt);
        this.PGIPerioder.removeIf(it -> it.getSkjæringstidspunkt().equals(skjæringstidspunkt));
    }

    void leggTil(PGIPeriode periode) {
        Objects.requireNonNull(periode);
        this.PGIPerioder.add(periode);
    }

    @Override
    public String toString() {
        return "PGIPerioder{" +
            "PGIPerioder=" + PGIPerioder +
            '}';
    }
}

