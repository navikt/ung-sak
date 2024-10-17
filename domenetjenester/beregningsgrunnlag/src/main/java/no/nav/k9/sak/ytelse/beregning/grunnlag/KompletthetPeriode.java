package no.nav.k9.sak.ytelse.beregning.grunnlag;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Immutable;

import no.nav.k9.kodeverk.beregningsgrunnlag.kompletthet.Vurdering;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.kodeverk.VurderingKodeverdiConverter;

@Entity(name = "KompletthetPeriode")
@Table(name = "BG_KOMPLETT_PERIODE")
@Immutable
public class KompletthetPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_KOMPLETT_PERIODE")
    private Long id;

    @Column(name = "skjaeringstidspunkt", nullable = false)
    private LocalDate skjæringstidspunkt;

    @Convert(converter = VurderingKodeverdiConverter.class)
    @Column(name = "vurdering", nullable = false)
    private Vurdering vurdering = Vurdering.UDEFINERT;

    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "vurdert_av")
    private String vurdertAv;

    @Column(name = "vurdert_tidspunkt")
    private LocalDateTime vurdertTidspunkt;

    public KompletthetPeriode() {
    }

    KompletthetPeriode(KompletthetPeriode grunnlagPeriode) {
        this.skjæringstidspunkt = grunnlagPeriode.skjæringstidspunkt;
        this.vurdering = grunnlagPeriode.vurdering;
        this.begrunnelse = grunnlagPeriode.begrunnelse;
        this.vurdertAv = grunnlagPeriode.getVurdertAv();
        this.vurdertTidspunkt = grunnlagPeriode.getVurdertTidspunkt();
    }

    public KompletthetPeriode(Vurdering vurdering, LocalDate skjæringstidspunkt, String begrunnelse, String vurdertAv, LocalDateTime vurdertTidspunkt) {
        this.skjæringstidspunkt = Objects.requireNonNull(skjæringstidspunkt);
        this.vurdering = Objects.requireNonNull(vurdering);
        this.begrunnelse = Objects.equals(Vurdering.UDEFINERT, vurdering) ? begrunnelse : Objects.requireNonNull(begrunnelse);
        this.vurdertAv = Objects.requireNonNull(vurdertAv);
        this.vurdertTidspunkt = Objects.requireNonNull(vurdertTidspunkt);
    }

    public Long getId() {
        return id;
    }


    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public Vurdering getVurdering() {
        return vurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getVurdertAv() {
        return vurdertAv;
    }

    public LocalDateTime getVurdertTidspunkt() {
        return vurdertTidspunkt;
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
        return "KompletthetPeriode{" +
            "id=" + id +
            ", utfallMerknad=" + vurdering +
            ", skjæringstidspunkt=" + skjæringstidspunkt +
            '}';
    }
}

