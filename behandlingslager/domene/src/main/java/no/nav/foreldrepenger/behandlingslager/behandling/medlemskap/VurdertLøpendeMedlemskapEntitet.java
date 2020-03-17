package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.ChangeTracked;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKeyComposer;
import no.nav.foreldrepenger.behandlingslager.kodeverk.MedlemskapManuellVurderingTypeKodeverdiConverter;
import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;

/**
 * Entitetsklasse for løpende medlemskap.
 */

@Entity(name = "VurdertLøpendeMedlemskap")
@Table(name = "MEDLEMSKAP_VURDERING_LOPENDE")
@DynamicInsert
@DynamicUpdate
public class VurdertLøpendeMedlemskapEntitet extends BaseEntitet implements VurdertMedlemskap, IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MEDLEMSKAP_VL")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vurdert_periode_id", nullable = false, updatable = false)
    private VurdertMedlemskapPeriodeEntitet periodeHolder;

    @Column(name = "oppholdsrett_vurdering")
    private Boolean oppholdsrettVurdering;

    @Column(name = "lovlig_opphold_vurdering")
    private Boolean lovligOppholdVurdering;

    @Column(name = "bosatt_vurdering")
    private Boolean bosattVurdering;

    @Column(name = "er_eos_borger")
    private Boolean erEøsBorger;

    @ChangeTracked
    @Column(name = "vurderingsdato", nullable = false, updatable = false)
    private LocalDate vurderingsdato;

    @ChangeTracked
    @Column(name = "begrunnelse")
    private String begrunnelse;

    @Convert(converter = MedlemskapManuellVurderingTypeKodeverdiConverter.class)
    @Column(name = "manuell_vurd", nullable = false)
    private MedlemskapManuellVurderingType medlemsperiodeManuellVurdering = MedlemskapManuellVurderingType.UDEFINERT;

    VurdertLøpendeMedlemskapEntitet() {
        // hibernate
    }

    /**
     * Copy ctor
     */
    VurdertLøpendeMedlemskapEntitet(VurdertLøpendeMedlemskapEntitet medlemskap) {
        this.oppholdsrettVurdering = medlemskap.getOppholdsrettVurdering();
        this.lovligOppholdVurdering = medlemskap.getLovligOppholdVurdering();
        this.bosattVurdering = medlemskap.getBosattVurdering();
        this.setMedlemsperiodeManuellVurdering(medlemskap.getMedlemsperiodeManuellVurdering());
        this.erEøsBorger = medlemskap.getErEøsBorger();
        this.vurderingsdato = medlemskap.getVurderingsdato();
        this.begrunnelse = medlemskap.getBegrunnelse();
    }

    @Override
    public Boolean getOppholdsrettVurdering() {
        return oppholdsrettVurdering;
    }

    void setOppholdsrettVurdering(Boolean oppholdsrettVurdering) {
        this.oppholdsrettVurdering = oppholdsrettVurdering;
    }

    void setVuderingsdato(LocalDate vuderingsdato) {
        this.vurderingsdato = vuderingsdato;
    }

    @Override
    public Boolean getLovligOppholdVurdering() {
        return lovligOppholdVurdering;
    }

    void setLovligOppholdVurdering(Boolean lovligOppholdVurdering) {
        this.lovligOppholdVurdering = lovligOppholdVurdering;
    }

    public LocalDate getVurderingsdato() {
        return vurderingsdato;
    }

    @Override
    public String getBegrunnelse() {
        return begrunnelse;
    }

    void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    @Override
    public Boolean getBosattVurdering() {
        return bosattVurdering;
    }

    void setBosattVurdering(Boolean bosattVurdering) {
        this.bosattVurdering = bosattVurdering;
    }

    @Override
    public MedlemskapManuellVurderingType getMedlemsperiodeManuellVurdering() {
        return Objects.equals(medlemsperiodeManuellVurdering, MedlemskapManuellVurderingType.UDEFINERT) ? null
            : medlemsperiodeManuellVurdering;
    }

    void setMedlemsperiodeManuellVurdering(MedlemskapManuellVurderingType medlemsperiodeManuellVurdering) {
        this.medlemsperiodeManuellVurdering = medlemsperiodeManuellVurdering == null ? MedlemskapManuellVurderingType.UDEFINERT
            : medlemsperiodeManuellVurdering;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<"
            + "vurderingsdato=" + vurderingsdato
            + ", medlemsperiodeManuellVurdering=" + medlemsperiodeManuellVurdering
            + ", oppholdsrettVurdering=" + oppholdsrettVurdering
            + ", lovligOppholdVurdering=" + lovligOppholdVurdering
            + ", erEøsBorger=" + erEøsBorger
            + ", begrunnelse=" + begrunnelse
            + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof VurdertLøpendeMedlemskapEntitet)) {
            return false;
        }
        VurdertLøpendeMedlemskapEntitet other = (VurdertLøpendeMedlemskapEntitet) obj;
        return Objects.equals(this.vurderingsdato, other.vurderingsdato);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(vurderingsdato);
    }

    @Override
    public Boolean getErEøsBorger() {
        return erEøsBorger;
    }

    void setErEøsBorger(Boolean erEøsBorger) {
        this.erEøsBorger = erEøsBorger;
    }

    public VurdertMedlemskapPeriodeEntitet getPeriodeHolder() {
        return periodeHolder;
    }

    void setPeriodeHolder(VurdertMedlemskapPeriodeEntitet periodeHolder) {
        this.periodeHolder = periodeHolder;
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { vurderingsdato };
        return IndexKeyComposer.createKey(keyParts);
    }
}
