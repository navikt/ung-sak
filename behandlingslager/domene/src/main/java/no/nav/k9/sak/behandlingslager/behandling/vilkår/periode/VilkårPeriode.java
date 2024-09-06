package no.nav.k9.sak.behandlingslager.behandling.vilkår.periode;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Clob;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.engine.jdbc.ClobProxy;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import no.nav.k9.felles.jpa.converters.PropertiesToStringConverter;
import no.nav.k9.kodeverk.api.IndexKey;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.DiffIgnore;
import no.nav.k9.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.k9.sak.behandlingslager.kodeverk.AvslagsårsakKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.UtfallKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.VurderUtfallMerknadKodeverdiConverter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Entity
@Table(name = "VR_VILKAR_PERIODE")
@DynamicInsert
@DynamicUpdate
public class VilkårPeriode extends BaseEntitet implements IndexKey, Comparable<VilkårPeriode> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_VILKAR_PERIODE")
    private Long id;

    @Column(name = "manuelt_vurdert", updatable = false, nullable = false)
    private boolean manueltVurdert = false;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom", nullable = false)),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom", nullable = false))
    })
    private DatoIntervallEntitet periode;

    @Convert(converter = UtfallKodeverdiConverter.class)
    @Column(name = "utfall", nullable = false)
    private Utfall utfall = Utfall.UDEFINERT;

    @Convert(converter = UtfallKodeverdiConverter.class)
    @Column(name = "overstyrt_utfall", nullable = false)
    private Utfall overstyrtUtfall = Utfall.UDEFINERT;

    @Convert(converter = VurderUtfallMerknadKodeverdiConverter.class)
    @Column(name = "merknad", nullable = false)
    private VilkårUtfallMerknad utfallMerknad = VilkårUtfallMerknad.UDEFINERT;

    @Convert(converter = PropertiesToStringConverter.class)
    @Column(name = "merknad_parametere")
    private Properties merknadParametere = new Properties();

    @Convert(converter = AvslagsårsakKodeverdiConverter.class)
    @Column(name = "avslag_kode", nullable = false)
    private Avslagsårsak avslagsårsak = Avslagsårsak.UDEFINERT;

    @Column(name = "BEGRUNNELSE")
    private String begrunnelse;

    @Lob
    @Column(name = "regel_evaluering")
    @DiffIgnore
    private Clob regelEvaluering;

    @Lob
    @Column(name = "regel_evaluering_oid")
    @DiffIgnore
    private Clob regelEvalueringNy;

    @Transient
    private transient AtomicReference<String> regelEvalueringCached = new AtomicReference<>();

    @Lob
    @Column(name = "regel_input")
    @DiffIgnore
    private Clob regelInput;

    @Lob
    @Column(name = "regel_input_oid")
    @DiffIgnore
    private Clob regelInputNy;

    @Transient
    private transient AtomicReference<String> regelInputCached = new AtomicReference<>();

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
        this.regelInputCached = vilkårPeriode.regelInputCached;
        this.regelEvaluering = vilkårPeriode.regelEvaluering;
        this.regelEvalueringCached = vilkårPeriode.regelEvalueringCached;
        this.regelInputNy = vilkårPeriode.regelInputNy;
        this.regelEvalueringNy = vilkårPeriode.regelEvalueringNy;

        this.begrunnelse = vilkårPeriode.begrunnelse;
    }

    private static String getPayload(Clob payload, AtomicReference<String> payloadStringRef) {
        var payloadString = payloadStringRef.get();
        if (payloadString != null && !payloadString.isBlank()) {
            return payloadString; // quick return, deserialisert tidligere
        }

        if (payload == null || (payloadString != null && payloadString.isEmpty())) {
            return null; // quick return, har ikke eller er tom
        }

        payloadString = ""; // dummy value for å signalisere at er allerede deserialisert
        try {
            BufferedReader in = new BufferedReader(payload.getCharacterStream());
            String line;
            StringBuilder sb = new StringBuilder(2048);
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            payloadString = sb.toString();
        } catch (SQLException | IOException e) {
            throw new PersistenceException("Kunne ikke lese payload: ", e);
        }
        payloadStringRef.set(payloadString);
        return payloadString;

    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = {periode};
        return IndexKeyComposer.createKey(keyParts);
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
     * Vilkårsperiodens skjæringstidspunkt (første dagen i perioden)
     *
     * @return skjæringdstidspunktet
     */
    public LocalDate getSkjæringstidspunkt() {
        return periode.getFomDato();
    }

    public LocalDate getFom() {
        return periode.getFomDato();
    }

    public LocalDate getTom() {
        return periode.getTomDato();
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
        if (getErOverstyrt()) {
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
        this.avslagsårsak = avslagsårsak == null ? Avslagsårsak.UDEFINERT : avslagsårsak;
    }

    void setManueltVurdert(boolean manueltVurdert) {
        this.manueltVurdert = manueltVurdert;
    }

    public String getRegelEvaluering() {
        if (regelEvalueringNy != null) {
            return getPayload(regelEvalueringNy, regelEvalueringCached);
        }
        return getPayload(regelEvaluering, regelEvalueringCached);
    }

    void setRegelEvaluering(String regelEvaluering) {
        if (this.id != null && this.regelEvaluering != null) {
            throw new IllegalStateException("Kan ikke overskrive regelEvaluering for VilkårPeriode: " + this.id);
        }
        this.regelEvalueringNy = regelEvaluering == null || regelEvaluering.isEmpty() ? null : ClobProxy.generateProxy(regelEvaluering);
        this.regelEvaluering = regelEvaluering == null || regelEvaluering.isEmpty() ? null : ClobProxy.generateProxy(regelEvaluering);
    }

    public String getRegelInput() {
        if (regelInputNy != null) {
            return getPayload(regelInputNy, regelInputCached);
        }
        return getPayload(regelInput, regelInputCached);
    }

    void setRegelInput(String regelInput) {
        if (this.id != null && this.regelInput != null) {
            throw new IllegalStateException("Kan ikke overskrive regelInput for VilkårPeriode: " + this.id);
        }
        this.regelInputNy = regelInput == null || regelInput.isEmpty() ? null : ClobProxy.generateProxy(regelInput);
        this.regelInput = regelInput == null || regelInput.isEmpty() ? null : ClobProxy.generateProxy(regelInput);
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

    @Override
    public int compareTo(VilkårPeriode o) {
        return getPeriode().compareTo(o.getPeriode());
    }
}
