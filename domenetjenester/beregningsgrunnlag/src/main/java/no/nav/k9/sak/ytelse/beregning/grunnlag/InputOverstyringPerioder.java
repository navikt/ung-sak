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

@Entity(name = "InputOverstyringPerioder")
@Table(name = "BG_OVST_INPUT_PERIODER")
@Immutable
public class InputOverstyringPerioder extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_OVST_INPUT_PERIODER")
    private Long id;

    @Immutable
    @JoinColumn(name = "BG_OVST_INPUT_ID", nullable = false, updatable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<InputOverstyringPeriode> inputOverstyringPerioder = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    InputOverstyringPerioder() {
    }

    InputOverstyringPerioder(InputOverstyringPerioder perioder) {
        if (perioder != null && perioder.inputOverstyringPerioder != null) {
            this.inputOverstyringPerioder = perioder.getInputOverstyringPerioder()
                .stream()
                .map(InputOverstyringPeriode::new)
                .collect(Collectors.toList());
        }
    }

    InputOverstyringPerioder(List<InputOverstyringPeriode> perioder) {
        this.inputOverstyringPerioder = perioder;
    }

    List<InputOverstyringPeriode> getInputOverstyringPerioder() {
        if (inputOverstyringPerioder == null) {
            return List.of();
        }
        return List.copyOf(inputOverstyringPerioder);
    }

    void deaktiver(LocalDate skjæringstidspunkt) {
        Objects.requireNonNull(skjæringstidspunkt);
        this.inputOverstyringPerioder.removeIf(it -> it.getSkjæringstidspunkt().equals(skjæringstidspunkt));
    }

    void leggTil(InputOverstyringPeriode periode) {
        Objects.requireNonNull(periode);
        this.inputOverstyringPerioder.add(periode);
    }

    @Override
    public String toString() {
        return "InputOverstyringPerioder{" +
            "id=" + id +
            ", perioder=" + inputOverstyringPerioder +
            '}';
    }
}

