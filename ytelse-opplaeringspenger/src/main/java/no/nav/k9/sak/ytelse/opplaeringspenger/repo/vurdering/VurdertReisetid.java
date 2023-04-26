package no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering;

import java.time.LocalDateTime;
import java.util.Objects;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "VurdertReisetid")
@Table(name = "olp_vurdert_reisetid")
@Immutable
public class VurdertReisetid extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OLP_VURDERT_REISETID")
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Column(name = "godkjent", nullable = false)
    private Boolean godkjent;

    @Column(name = "begrunnelse", nullable = false)
    private String begrunnelse;

    @Column(name = "vurdert_av", nullable = false)
    private String vurdertAv;

    @Column(name = "vurdert_tid", nullable = false)
    private LocalDateTime vurdertTidspunkt;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VurdertReisetid() {
    }

    public VurdertReisetid(DatoIntervallEntitet periode, Boolean godkjent, String begrunnelse, String vurdertAv, LocalDateTime vurdertTidspunkt) {
        this.periode = periode;
        this.godkjent = godkjent;
        this.begrunnelse = begrunnelse;
        this.vurdertAv = vurdertAv;
        this.vurdertTidspunkt = vurdertTidspunkt;
    }

    public VurdertReisetid(VurdertReisetid that) {
        this.periode = that.periode;
        this.godkjent = that.godkjent;
        this.begrunnelse = that.begrunnelse;
        this.vurdertAv = that.vurdertAv;
        this.vurdertTidspunkt = that.vurdertTidspunkt;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public Boolean getGodkjent() {
        return godkjent;
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
        VurdertReisetid that = (VurdertReisetid) o;
        return Objects.equals(periode, that.periode)
            && Objects.equals(godkjent, that.godkjent)
            && Objects.equals(begrunnelse, that.begrunnelse)
            && Objects.equals(vurdertAv, that.vurdertAv)
            && Objects.equals(vurdertTidspunkt, that.vurdertTidspunkt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, godkjent, begrunnelse, vurdertAv, vurdertTidspunkt);
    }
}
