package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.bosatt.OpphørKilde;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

import java.time.LocalDate;
import java.util.Objects;

@Entity(name = "OpphørResultat")
@Table(name = "rs_opphoer")
public class OpphørResultat extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RS_OPPHOER")
    @SequenceGenerator(name = "SEQ_RS_OPPHOER", sequenceName = "seq_rs_opphoer", allocationSize = 50)
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Column(name = "skaeringstidspunkt", nullable = false, updatable = false)
    private LocalDate skjæringstidspunkt;

    @Column(name = "opphors_dato")
    private LocalDate opphørDato;

    @Enumerated(EnumType.STRING)
    @Column(name = "opphors_aarsak")
    private Avslagsårsak opphørÅrsak;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "vilkar_type", nullable = false, updatable = false)
    private VilkårType vilkårType;

    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Column(name = "fritekst_vurdering_brev")
    private String fritekstVurderingBrev;

    @Enumerated(EnumType.STRING)
    @Column(name = "kilde", nullable = false, updatable = false)
    private OpphørKilde kilde;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    OpphørResultat() {
        // Hibernate
    }

    public OpphørResultat(Long behandlingId, LocalDate skjæringstidspunkt, LocalDate opphørDato, Avslagsårsak opphørÅrsak, OpphørKilde kilde, VilkårType vilkårType) {
        this.behandlingId = Objects.requireNonNull(behandlingId);
        this.skjæringstidspunkt = Objects.requireNonNull(skjæringstidspunkt);
        this.opphørDato = opphørDato;
        this.opphørÅrsak = opphørÅrsak;
        this.kilde = Objects.requireNonNull(kilde);
        this.vilkårType = Objects.requireNonNull(vilkårType);
    }

    public OpphørResultat(Long behandlingId, LocalDate skjæringstidspunkt, LocalDate opphørDato, Avslagsårsak opphørÅrsak, OpphørKilde kilde, VilkårType vilkårType, String begrunnelse, String fritekstVurderingBrev) {
        this(behandlingId, skjæringstidspunkt, opphørDato, opphørÅrsak, kilde, vilkårType);
        this.begrunnelse = begrunnelse;
        this.fritekstVurderingBrev = fritekstVurderingBrev;
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }

    public LocalDate getOpphørDato() {
        return opphørDato;
    }

    public Avslagsårsak getOpphørÅrsak() {
        return opphørÅrsak;
    }

    public OpphørKilde getKilde() {
        return kilde;
    }

    public VilkårType getVilkårType() {
        return vilkårType;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    public String getFritekstVurderingBrev() {
        return fritekstVurderingBrev;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OpphørResultat that)) return false;
        return Objects.equals(behandlingId, that.behandlingId)
            && Objects.equals(skjæringstidspunkt, that.skjæringstidspunkt)
            && vilkårType == that.vilkårType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, skjæringstidspunkt, vilkårType);
    }

    @Override
    public String toString() {
        return "OpphørResultat{behandlingId=" + behandlingId
            + ", skjæringstidspunkt=" + skjæringstidspunkt
            + ", opphørDato=" + opphørDato
            + ", opphørÅrsak=" + opphørÅrsak
            + ", vilkårType=" + vilkårType
            + ", kilde=" + kilde
            + ", aktiv=" + aktiv + '}';
    }
}
