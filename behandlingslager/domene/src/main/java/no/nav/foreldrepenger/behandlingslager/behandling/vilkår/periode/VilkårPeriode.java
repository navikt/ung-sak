package no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode;

import java.util.Objects;
import java.util.Properties;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Utfall;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.diff.IndexKey;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.jpa.converters.PropertiesToStringConverter;

@Entity
@Table(name = "VR_VILKAR_PERIODE")
public class VilkårPeriode extends BaseEntitet implements IndexKey {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VILKAR_PERIODE")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vilkar_id", nullable = false, updatable = false)
    private Vilkår vilkår;

    @Column(name = "manuelt_vurdert", updatable = false, nullable = false)
    private boolean manueltVurdert = false;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Convert(converter = Utfall.KodeverdiConverter.class)
    @Column(name = "utfall", nullable = false)
    private Utfall utfall = Utfall.UDEFINERT;

    @Convert(converter = Utfall.KodeverdiConverter.class)
    @Column(name = "overstyrt_utfall", nullable = false)
    private Utfall overstyrtUtfall = Utfall.UDEFINERT;

    @Convert(converter = VilkårUtfallMerknad.KodeverdiConverter.class)
    @Column(name = "merknad", nullable = false)
    private VilkårUtfallMerknad utfallMerknad = VilkårUtfallMerknad.UDEFINERT;

    @Convert(converter = PropertiesToStringConverter.class)
    @Column(name = "merknad_parametere")
    private Properties merknadParametere = new Properties();

    @Convert(converter = Avslagsårsak.KodeverdiConverter.class)
    @Column(name = "avslag_kode", nullable = false)
    private Avslagsårsak avslagsårsak = Avslagsårsak.UDEFINERT;

    @Column(name = "BEGRUNNELSE")
    private String begrunnelse;

    @Lob
    @Column(name = "regel_evaluering", columnDefinition = "text")
    @Basic(fetch = FetchType.LAZY)
    private String regelEvaluering;

    @Lob
    @Column(name = "regel_input", columnDefinition = "text")
    @Basic(fetch = FetchType.LAZY)
    private String regelInput;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VilkårPeriode() {
    }

    // Benyttes til å kopieres inn i nye behandlinger
    public VilkårPeriode(VilkårPeriode vilkårPeriode) {
        this.periode = vilkårPeriode.periode;
        this.utfall = vilkårPeriode.utfall;
        this.overstyrtUtfall = vilkårPeriode.overstyrtUtfall;
        this.manueltVurdert = vilkårPeriode.manueltVurdert;
        this.merknadParametere = vilkårPeriode.merknadParametere;
        this.avslagsårsak = vilkårPeriode.avslagsårsak;
        this.utfallMerknad = vilkårPeriode.utfallMerknad;
        this.regelInput = vilkårPeriode.regelInput;
        this.regelEvaluering = vilkårPeriode.regelEvaluering;
    }

    @Override
    public String getIndexKey() {
        return IndexKey.createKey(periode);
    }

    /**
     * Returnerer true om vilkårets utfall har blitt manuelt vurdert.
     * NB! Dette gjelder har ingenting med om vilkåret har vært overstyrt.
     * For dette se {@link #getErOverstyrt()}
     *
     * @return true / false
     */
    public boolean getErManueltVurdert() {
        return manueltVurdert;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    /**
     * Gir det mest relevante utfallet avhengig av hva som er registrert.
     * <ol>
     * <li>Automatisk utfall</li>
     * <li>Manuelt utfall</li>
     * <li>Overstyrt utfall</li>
     * </ol>
     *
     * @return VilkårUtfallType
     */
    public Utfall getGjeldendeUtfall() {
        if (!overstyrtUtfall.equals(Utfall.UDEFINERT)) {
            return overstyrtUtfall;
        }
        return utfall;
    }

    public boolean getErOverstyrt() {
        return !overstyrtUtfall.equals(Utfall.UDEFINERT);
    }

    public Utfall getUtfall() {
        return utfall;
    }

    void setUtfall(Utfall utfall) {
        this.utfall = utfall;
    }

    public Utfall getOverstyrtUtfall() {
        return overstyrtUtfall;
    }

    void setOverstyrtUtfall(Utfall overstyrtUtfall) {
        this.overstyrtUtfall = overstyrtUtfall;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public VilkårUtfallMerknad getMerknad() {
        return utfallMerknad;
    }

    void setUtfallMerknad(VilkårUtfallMerknad utfallMerknad) {
        this.utfallMerknad = utfallMerknad != null ? utfallMerknad : VilkårUtfallMerknad.UDEFINERT;
    }

    public Properties getMerknadParametere() {
        return merknadParametere;
    }

    void setMerknadParametere(Properties merknadParametere) {
        this.merknadParametere = merknadParametere;
    }

    public Avslagsårsak getAvslagsårsak() {
        if (getGjeldendeUtfall().equals(Utfall.IKKE_OPPFYLT)) {
            return avslagsårsak;
        }
        return null;
    }

    void setAvslagsårsak(Avslagsårsak avslagsårsak) {
        this.avslagsårsak = avslagsårsak;
    }

    void setManueltVurdert(boolean manueltVurdert) {
        this.manueltVurdert = manueltVurdert;
    }

    public void setVilkår(Vilkår vilkår) {
        this.vilkår = vilkår;
    }

    public String getRegelEvaluering() {
        return regelEvaluering;
    }

    void setRegelEvaluering(String regelEvaluering) {
        this.regelEvaluering = regelEvaluering;
    }

    public String getRegelInput() {
        return regelInput;
    }

    void setRegelInput(String regelInput) {
        this.regelInput = regelInput;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VilkårPeriode that = (VilkårPeriode) o;
        return manueltVurdert == that.manueltVurdert &&
            utfall == that.utfall &&
            Objects.equals(begrunnelse, that.begrunnelse) &&
            avslagsårsak == that.avslagsårsak &&
            utfallMerknad == that.utfallMerknad &&
            merknadParametere == that.merknadParametere &&
            overstyrtUtfall == that.overstyrtUtfall;
    }

    @Override
    public int hashCode() {
        return Objects.hash(manueltVurdert, utfall, begrunnelse, avslagsårsak, utfallMerknad, merknadParametere, overstyrtUtfall);
    }

    @Override
    public String toString() {
        return "VilkårPeriode{" +
            "manueltVurdert=" + manueltVurdert +
            ", periode=" + periode +
            ", utfall=" + utfall +
            ", overstyrtUtfall=" + overstyrtUtfall +
            '}';
    }
}
