package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

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
import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity(name = "KursPeriode")
@Table(name = "UP_KURS_PERIODE")
@Immutable
public class KursPeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UP_KURS_PERIODE")
    private Long id;

    @ChangeTracked
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @ChangeTracked
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "reise_til_fom")),
        @AttributeOverride(name = "tomDato", column = @Column(name = "reise_til_tom"))
    })
    private DatoIntervallEntitet reiseperiodeTil;

    @ChangeTracked
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "reise_hjem_fom")),
        @AttributeOverride(name = "tomDato", column = @Column(name = "reise_hjem_tom"))
    })
    private DatoIntervallEntitet reiseperiodeHjem;

    @Column(name = "begrunnelse_reisetid_til")
    private String begrunnelseReisetidTil;

    @Column(name = "begrunnelse_reisetid_hjem")
    private String begrunnelseReisetidHjem;

    @Column(name = "institusjon")
    private String institusjon;

    @Column(name = "institusjon_uuid")
    private UUID institusjonUuid;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    KursPeriode() {
    }

    public KursPeriode(DatoIntervallEntitet periode, DatoIntervallEntitet reiseperiodeTil, DatoIntervallEntitet reiseperiodeHjem, String institusjon, UUID institusjonUuid, String begrunnelseReisetidTil, String begrunnelseReisetidHjem) {
        this.periode = periode;
        this.reiseperiodeTil = reiseperiodeTil;
        this.reiseperiodeHjem = reiseperiodeHjem;
        this.institusjon = institusjon;
        this.institusjonUuid = institusjonUuid;
        this.begrunnelseReisetidTil = begrunnelseReisetidTil;
        this.begrunnelseReisetidHjem = begrunnelseReisetidHjem;
    }

    public KursPeriode(LocalDate fom, LocalDate tom, DatoIntervallEntitet reiseperiodeTil, DatoIntervallEntitet reiseperiodeHjem, String institusjon, UUID institusjonUuid, String begrunnelseReisetidTil, String begrunnelseReisetidHjem) {
        this(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), reiseperiodeTil, reiseperiodeHjem, institusjon, institusjonUuid, begrunnelseReisetidTil, begrunnelseReisetidHjem);
    }

    public KursPeriode(KursPeriode it) {
        this.periode = it.periode;
        this.institusjon = it.institusjon;
        this.reiseperiodeHjem = it.reiseperiodeHjem;
        this.reiseperiodeTil = it.reiseperiodeTil;
        this.institusjonUuid = it.institusjonUuid;
        this.begrunnelseReisetidTil = it.begrunnelseReisetidTil;
        this.begrunnelseReisetidHjem = it.begrunnelseReisetidHjem;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public String getInstitusjon() {
        return institusjon;
    }

    public DatoIntervallEntitet getReiseperiodeTil() {
        return reiseperiodeTil;
    }

    public DatoIntervallEntitet getReiseperiodeHjem() {
        return reiseperiodeHjem;
    }

    public UUID getInstitusjonUuid() {
        return institusjonUuid;
    }

    public String getBegrunnelseReisetidTil() {
        return begrunnelseReisetidTil;
    }

    public String getBegrunnelseReisetidHjem() {
        return begrunnelseReisetidHjem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KursPeriode that = (KursPeriode) o;
        return Objects.equals(periode, that.periode)
            && Objects.equals(institusjon, that.institusjon)
            && Objects.equals(reiseperiodeTil, that.reiseperiodeTil)
            && Objects.equals(reiseperiodeHjem, that.reiseperiodeHjem)
            && Objects.equals(begrunnelseReisetidTil, that.begrunnelseReisetidTil)
            && Objects.equals(begrunnelseReisetidHjem, that.begrunnelseReisetidHjem)
            && Objects.equals(institusjonUuid, that.institusjonUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, institusjon, reiseperiodeTil, reiseperiodeHjem, institusjonUuid, begrunnelseReisetidTil, begrunnelseReisetidHjem);
    }

    @Override
    public String getIndexKey() {
        return IndexKeyComposer.createKey(institusjon, periode, reiseperiodeTil, reiseperiodeHjem, institusjonUuid, begrunnelseReisetidTil, begrunnelseReisetidHjem);
    }

    @Override
    public String toString() {
        return "KursPeriode{" +
            "periode=" + periode +
            ", institusjon=" + institusjon +
            ", institusjonUuid=" + institusjonUuid +
            ", reiseperiodeTil=" + reiseperiodeTil +
            ", reiseperiodeHjem=" + reiseperiodeHjem +
            ", begrunnelseReisetidTil=" + begrunnelseReisetidTil +
            ", begrunnelseReisetidHjem=" + begrunnelseReisetidHjem +
            '}';
    }
}
