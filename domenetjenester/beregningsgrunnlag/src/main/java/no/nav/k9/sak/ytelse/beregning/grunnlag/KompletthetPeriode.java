package no.nav.k9.sak.ytelse.beregning.grunnlag;


import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.kodeverk.VurderingKodeverdiConverter;

@Entity(name = "KompletthetPeriode")
@Table(name = "SEQ_BG_KOMPLETT_PERIODE")
@Immutable
public class KompletthetPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SEQ_BG_KOMPLETT_PERIODE")
    private Long id;

    @Column(name = "skjaeringstidspunkt", nullable = false)
    private LocalDate skjæringstidspunkt;

    @Convert(converter = VurderingKodeverdiConverter.class)
    @Column(name = "merknad", nullable = false)
    private Vurdering utfallMerknad = Vurdering.UDEFINERT;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public KompletthetPeriode() {
    }

    KompletthetPeriode(KompletthetPeriode grunnlagPeriode) {
        this.skjæringstidspunkt = grunnlagPeriode.skjæringstidspunkt;
    }

    public KompletthetPeriode(UUID eksternReferanse, LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = Objects.requireNonNull(skjæringstidspunkt);
    }

    public Long getId() {
        return id;
    }


    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KompletthetPeriode that = (KompletthetPeriode) o;
        return Objects.equals(skjæringstidspunkt, that.skjæringstidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skjæringstidspunkt);
    }

    @Override
    public String toString() {
        return "BeregningsgrunnlagPeriode{" +
            "id=" + id +
            ", eksternReferanse=" + null +
            ", skjæringstidspunkt=" + skjæringstidspunkt +
            '}';
    }
}

