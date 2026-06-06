package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.PostgreSQLRangeType;
import no.nav.ung.sak.domene.typer.tid.Range;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Immutable
@Entity(name = "BistandsvilkårVurderingPeriode")
@Table(name = "inngangsvilkaar_bistand_vurd_periode")
public class BistandsvilkårVurderingPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_INNGANGSVILKAAR_BISTAND_VURD_PERIODE")
    @SequenceGenerator(name = "SEQ_INNGANGSVILKAAR_BISTAND_VURD_PERIODE", sequenceName = "seq_inngangsvilkaar_bistand_vurd_periode", allocationSize = 50)
    private Long id;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange", nullable = false, updatable = false)
    private Range<LocalDate> periode;

    @Column(name = "godkjent", nullable = false, updatable = false)
    private boolean godkjent;

    @Column(name = "avslagsarsak", updatable = false)
    private String avslagsårsakKode;

    @Column(name = "vurdert_av", nullable = false, updatable = false)
    private String vurdertAv;

    @Column(name = "vurdert_tidspunkt", nullable = false, updatable = false)
    private LocalDateTime vurdertTidspunkt;

    BistandsvilkårVurderingPeriode() {
        // Hibernate
    }

    public BistandsvilkårVurderingPeriode(DatoIntervallEntitet periode, boolean godkjent, Avslagsårsak avslagsårsak, String vurdertAv, LocalDateTime vurdertTidspunkt) {
        Objects.requireNonNull(periode, "periode");
        Objects.requireNonNull(vurdertAv, "vurdertAv");
        Objects.requireNonNull(vurdertTidspunkt, "vurdertTidspunkt");
        if (!godkjent) {
            Objects.requireNonNull(avslagsårsak, "avslagsårsak må settes når godkjent=false");
        }
        this.periode = Range.closed(periode.getFomDato(), periode.getTomDato());
        this.godkjent = godkjent;
        this.avslagsårsakKode = avslagsårsak != null ? avslagsårsak.getKode() : null;
        this.vurdertAv = vurdertAv;
        this.vurdertTidspunkt = vurdertTidspunkt;
    }

    public Long getId() {
        return id;
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public boolean isGodkjent() {
        return godkjent;
    }

    public Avslagsårsak getAvslagsårsak() {
        return avslagsårsakKode != null ? Avslagsårsak.fraKode(avslagsårsakKode) : null;
    }

    public String getVurdertAv() {
        return vurdertAv;
    }

    public LocalDateTime getVurdertTidspunkt() {
        return vurdertTidspunkt;
    }
}
