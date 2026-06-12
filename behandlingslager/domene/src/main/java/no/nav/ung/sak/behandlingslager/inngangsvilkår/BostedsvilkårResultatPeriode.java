package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
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
@Entity(name = "BostedsvilkårResultatPeriode")
@Table(name = "bosted_resultat_periode")
public class BostedsvilkårResultatPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSTED_RESULTAT_PERIODE")
    @SequenceGenerator(name = "SEQ_BOSTED_RESULTAT_PERIODE", sequenceName = "seq_bosted_resultat_periode", allocationSize = 50)
    private Long id;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange", nullable = false, updatable = false)
    private Range<LocalDate> periode;

    @Column(name = "godkjent", nullable = false, updatable = false)
    private boolean godkjent;

    @Enumerated(EnumType.STRING)
    @Column(name = "ikke_oppfylt_aarsak", updatable = false)
    private BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak;

    @Column(name = "manuell_vurdering", nullable = false, updatable = false)
    private boolean manuellVurdering;

    @Column(name = "begrunnelse", updatable = false)
    private String begrunnelse;

    @Column(name = "fritekst_vurdering_brev", updatable = false)
    private String fritekstVurderingBrev;

    @Column(name = "vurdert_av", nullable = false, updatable = false)
    private String vurdertAv;

    @Column(name = "vurdert_tidspunkt", nullable = false, updatable = false)
    private LocalDateTime vurdertTidspunkt;

    protected BostedsvilkårResultatPeriode() {
        // Hibernate
    }

    /** Oppretter en kopi med ny periode, men med verdiene fra kildeentiteten. Brukes ved sammenslåing av tidslinjer. */
    BostedsvilkårResultatPeriode(DatoIntervallEntitet periode, BostedsvilkårResultatPeriode kilde) {
        this(periode, kilde.godkjent, kilde.ikkeOppfyltÅrsak, kilde.manuellVurdering, kilde.begrunnelse, kilde.fritekstVurderingBrev, kilde.vurdertAv, kilde.vurdertTidspunkt);
    }

    public BostedsvilkårResultatPeriode(DatoIntervallEntitet periode, boolean godkjent, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, boolean manuellVurdering, String begrunnelse, String fritekstVurderingBrev, String vurdertAv, LocalDateTime vurdertTidspunkt) {
        Objects.requireNonNull(periode, "periode");
        Objects.requireNonNull(vurdertAv, "vurdertAv");
        Objects.requireNonNull(vurdertTidspunkt, "vurdertTidspunkt");
        if (!godkjent) {
            Objects.requireNonNull(ikkeOppfyltÅrsak, "ikkeOppfyltÅrsak må settes når godkjent=false");
        }
        this.periode = Range.closed(periode.getFomDato(), periode.getTomDato());
        this.godkjent = godkjent;
        this.ikkeOppfyltÅrsak = ikkeOppfyltÅrsak;
        this.manuellVurdering = manuellVurdering;
        this.begrunnelse = begrunnelse;
        this.fritekstVurderingBrev = fritekstVurderingBrev;
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

    public BostedsvilkårIkkeOppfyltÅrsak getIkkeOppfyltÅrsak() {
        return ikkeOppfyltÅrsak;
    }

    public boolean isManuellVurdering() {
        return manuellVurdering;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstVurderingBrev() {
        return fritekstVurderingBrev;
    }

    public String getVurdertAv() {
        return vurdertAv;
    }

    public LocalDateTime getVurdertTidspunkt() {
        return vurdertTidspunkt;
    }
}

