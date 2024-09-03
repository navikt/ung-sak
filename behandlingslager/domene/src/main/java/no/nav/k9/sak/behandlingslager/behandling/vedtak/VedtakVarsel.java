package no.nav.k9.sak.behandlingslager.behandling.vedtak;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.engine.jdbc.ClobProxy;

import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.sak.behandlingslager.BaseEntitet;

@Entity(name = "VedtakVarsel")
@Table(name = "BEHANDLING_VEDTAK_VARSEL")
@DynamicInsert
@DynamicUpdate
public class VedtakVarsel extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BEHANDLING_VEDTAK_VARSEL")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    /* bruker @ManyToOne siden JPA ikke støtter OneToOne join på non-PK column. */
    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Convert(converter = VedtakKodeverdiConverter.class)
    @Column(name = "vedtaksbrev", nullable = false)
    private Vedtaksbrev vedtaksbrev = Vedtaksbrev.UDEFINERT;

    @Column(name = "avslag_arsak_fritekst")
    private String avslagarsakFritekst;

    @Column(name = "overskrift")
    private String overskrift;

    @Lob
    @Column(name = "fritekstbrev")
    private String fritekstbrev;

    @Transient
    private String fritekstbrevString;

    @Lob
    @Column(name = "fritekstbrev_oid")
    private Clob fritekstbrevNy;

    @Column(name = "sendt_varsel_om_revurdering")
    private Boolean harSendtVarselOmRevurdering;

    @Column(name = "redusert_utbetaling_aarsaker")
    @Convert(converter = StringSetConverter.class)
    private Set<String> redusertUtbetalingÅrsaker = Collections.emptySet();


    public VedtakVarsel() {
        // for hibernate
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public String getAvslagarsakFritekst() {
        return avslagarsakFritekst;
    }

    public void setAvslagarsakFritekst(String avslagarsakFritekst) {
        this.avslagarsakFritekst = avslagarsakFritekst;
    }

    public Vedtaksbrev getVedtaksbrev() {
        return vedtaksbrev;
    }

    public String getOverskrift() {
        return overskrift;
    }

    public String getFritekstbrev() {
        if (fritekstbrevNy != null) {
            getFritekstbrevString();
        }
        return fritekstbrev;
    }

    public String getFritekstbrevString() {
        if (fritekstbrevString != null && !fritekstbrevString.isEmpty()) {
            return fritekstbrevString; // quick return, deserialisert tidligere
        }
        if (fritekstbrevNy == null || (fritekstbrevString != null && fritekstbrevString.isEmpty())) {
            return null; // quick return, har ikke eller er tom
        }

        fritekstbrevString = ""; // dummy value for å signalisere at er allerede deserialisert

        try {
            BufferedReader in = new BufferedReader(fritekstbrevNy.getCharacterStream());
            String line;
            StringBuilder sb = new StringBuilder(2048);
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            fritekstbrevString = sb.toString();
        } catch (SQLException | IOException e) {
            throw new PersistenceException("Kunne ikke lese payload: ", e);
        }
        return fritekstbrevString;
    }

    public void setOverskrift(String overskrift) {
        this.overskrift = overskrift;
    }

    public void setFritekstbrev(String fritekstbrev) {
        this.fritekstbrevNy = fritekstbrev == null ? null : ClobProxy.generateProxy(fritekstbrev);
        this.fritekstbrev = fritekstbrev;
    }

    public void setVedtaksbrev(Vedtaksbrev vedtaksbrev) {
        this.vedtaksbrev = vedtaksbrev;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<>";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VedtakVarsel)) {
            return false;
        }
        VedtakVarsel that = (VedtakVarsel) o;
        // Behandlingsresultat skal p.t. kun eksisterere dersom parent Behandling allerede er persistert.
        // Det syntaktisk korrekte vil derfor være at subaggregat Behandlingsresultat med 1:1-forhold til parent
        // Behandling har også sin id knyttet opp mot Behandling alene.
        return getBehandlingId().equals(that.getBehandlingId());
    }

    public void setBehandlingId(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBehandlingId());
    }

    public void setHarSendtVarselOmRevurdering(Boolean harSendtVarselOmRevurdering) {
        this.harSendtVarselOmRevurdering = harSendtVarselOmRevurdering;
    }

    public boolean getErVarselOmRevurderingSendt() {
        return Boolean.TRUE.equals(harSendtVarselOmRevurdering);
    }

    public Set<String> getRedusertUtbetalingÅrsaker() {
        return Collections.unmodifiableSet(redusertUtbetalingÅrsaker);
    }

    public void setRedusertUtbetalingÅrsaker(Set<String> redusertUtbetalingÅrsaker) {
        this.redusertUtbetalingÅrsaker = redusertUtbetalingÅrsaker == null ? Collections.emptySet() : Set.copyOf(redusertUtbetalingÅrsaker);
    }

    @Converter
    static class StringSetConverter implements AttributeConverter<Set<String>, String> {

        private static final String SPLIT_CHAR = ",";

        @Override
        public String convertToDatabaseColumn(Set<String> set) {
            return set.isEmpty() ? null : String.join(",", set);
        }

        @Override
        public Set<String> convertToEntityAttribute(String joined) {
            return joined == null ? Collections.emptySet() : Set.of(joined.split(SPLIT_CHAR));
        }

    }

}
