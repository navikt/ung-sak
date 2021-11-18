package no.nav.k9.sak.ytelse.beregning.grunnlag;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

@Entity(name = "InputOverstyringPeriode")
@Table(name = "BG_OVST_INPUT_PERIODE")
@Immutable
public class InputOverstyringPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_OVST_INPUT_PERIODE")
    private Long id;

    @Column(name = "skjaeringstidspunkt", nullable = false)
    private LocalDate skjæringstidspunkt;

    @Immutable
    @JoinColumn(name = "BG_OVST_INPUT_PERIODE_ID", nullable = false, updatable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private List<InputAktivitetOverstyring> aktivitetOverstyringer = new ArrayList<>();

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public InputOverstyringPeriode() {
    }

    public InputOverstyringPeriode(InputOverstyringPeriode inputOverstyring) {
        this.skjæringstidspunkt = inputOverstyring.getSkjæringstidspunkt();
        this.aktivitetOverstyringer = inputOverstyring.getAktivitetOverstyringer().stream()
            .map(InputAktivitetOverstyring::new)
            .collect(Collectors.toList());
    }


    public InputOverstyringPeriode(LocalDate skjæringstidspunkt, List<InputAktivitetOverstyring> aktivitetOverstyringer) {
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.aktivitetOverstyringer = aktivitetOverstyringer;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public List<InputAktivitetOverstyring> getAktivitetOverstyringer() {
        return aktivitetOverstyringer;
    }
}
