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

import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "KompletthetPerioder")
@Table(name = "BG_KOMPLETT_PERIODER")
@Immutable
class KompletthetPerioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_KOMPLETT_PERIODER")
    private Long id;

    @Immutable
    @JoinColumn(name = "bg_komplett_id", nullable = false, updatable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<KompletthetPeriode> kompletthetPerioder = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    KompletthetPerioder() {
    }

    KompletthetPerioder(KompletthetPerioder grunnlagPeriode) {
        if (grunnlagPeriode != null && grunnlagPeriode.kompletthetPerioder != null) {
            this.kompletthetPerioder = grunnlagPeriode.getKompletthetPerioder()
                .stream()
                .map(KompletthetPeriode::new)
                .collect(Collectors.toList());
        }
    }

    KompletthetPerioder(List<KompletthetPeriode> kompletthetPerioder) {
        this.kompletthetPerioder = kompletthetPerioder;
    }

    List<KompletthetPeriode> getKompletthetPerioder() {
        if (kompletthetPerioder == null) {
            return List.of();
        }
        return List.copyOf(kompletthetPerioder);
    }

    void deaktiver(LocalDate skjæringstidspunkt) {
        Objects.requireNonNull(skjæringstidspunkt);
        this.kompletthetPerioder.removeIf(it -> it.getSkjæringstidspunkt().equals(skjæringstidspunkt));
    }

    void leggTil(KompletthetPeriode periode) {
        Objects.requireNonNull(periode);
        this.kompletthetPerioder.add(periode);
    }

    @Override
    public String toString() {
        return "KompletthetPerioder{" +
            "id=" + id +
            ", perioder=" + kompletthetPerioder +
            '}';
    }
}

