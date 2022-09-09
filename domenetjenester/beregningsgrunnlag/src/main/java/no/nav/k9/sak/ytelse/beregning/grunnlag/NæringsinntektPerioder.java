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

@Entity(name = "NæringsinntektPerioder")
@Table(name = "BG_NAERING_INNTEKT_PERIODER")
@Immutable
class NæringsinntektPerioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_NAERING_INNTEKT_PERIOOER")
    private Long id;

    @Immutable
    @JoinColumn(name = "bg_naering_inntekt_id", nullable = false, updatable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<NæringsinntektPeriode> næringsinntektPerioder = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    NæringsinntektPerioder() {
    }

    NæringsinntektPerioder(NæringsinntektPerioder inntektPerioder) {
        if (inntektPerioder != null && inntektPerioder.næringsinntektPerioder != null) {
            this.næringsinntektPerioder = inntektPerioder.getNæringsinntektPerioder().stream().map(NæringsinntektPeriode::new).collect(Collectors.toList());
        }
    }

    NæringsinntektPerioder(List<NæringsinntektPeriode> næringsinntektPerioder) {
        this.næringsinntektPerioder = næringsinntektPerioder;
    }

    List<NæringsinntektPeriode> getNæringsinntektPerioder() {
        if (næringsinntektPerioder == null) {
            return List.of();
        }
        return List.copyOf(næringsinntektPerioder);
    }

    void deaktiver(LocalDate skjæringstidspunkt) {
        Objects.requireNonNull(skjæringstidspunkt);
        this.næringsinntektPerioder.removeIf(it -> it.getSkjæringstidspunkt().equals(skjæringstidspunkt));
    }

    void leggTil(NæringsinntektPeriode periode) {
        Objects.requireNonNull(periode);
        this.næringsinntektPerioder.add(periode);
    }

    @Override
    public String toString() {
        return "NæringsinntektPerioder{" +
            "næringsinntektPerioder=" + næringsinntektPerioder +
            '}';
    }
}

