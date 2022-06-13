package no.nav.k9.sak.behandlingslager.fagsak;

import java.time.LocalDate;
import java.util.Objects;

import org.hibernate.annotations.Type;

import com.vladmihalcea.hibernate.type.range.PostgreSQLRangeType;
import com.vladmihalcea.hibernate.type.range.Range;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreRemove;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.kodeverk.FagsakStatusKodeverdiConverter;
import no.nav.k9.sak.behandlingslager.kodeverk.FagsakYtelseTypeKodeverdiConverter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@Entity(name = "Fagsak")
@Table(name = "FAGSAK")
public class Fagsak extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_FAGSAK")
    @Column(name = "id")
    private Long id;

    @Convert(converter = FagsakYtelseTypeKodeverdiConverter.class)
    @Column(name = "ytelse_type", nullable = false, updatable = false)
    private FagsakYtelseType ytelseType = FagsakYtelseType.UDEFINERT;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "bruker_aktoer_id", unique = true, nullable = false, updatable = false)))
    private AktørId brukerAktørId;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "pleietrengende_aktoer_id", unique = true, nullable = true, updatable = true)))
    private AktørId pleietrengendeAktørId;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "relatert_person_aktoer_id", unique = true, nullable = true, updatable = true)))
    private AktørId relatertPersonAktørId;

    @Convert(converter = FagsakStatusKodeverdiConverter.class)
    @Column(name = "fagsak_status", nullable = false)
    private FagsakStatus fagsakStatus = FagsakStatus.DEFAULT;

    /**
     * Offisielt tildelt saksnummer for GSAK.
     */
    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "saksnummer", column = @Column(name = "saksnummer")))
    private Saksnummer saksnummer;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Column(name = "til_infotrygd", nullable = false)
    private boolean skalTilInfotrygd = false;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Fagsak() {
        // Hibernate
    }

    public Fagsak(FagsakYtelseType ytelseType, AktørId søker, AktørId pleietrengende, AktørId relatertPerson, Saksnummer saksnummer, LocalDate fom, LocalDate tom) {
        Objects.requireNonNull(ytelseType, "ytelseType");
        this.ytelseType = ytelseType;
        this.brukerAktørId = søker;
        this.pleietrengendeAktørId = pleietrengende;
        this.relatertPersonAktørId = relatertPerson;
        if (saksnummer != null) {
            setSaksnummer(saksnummer);
        }
        setPeriode(fom, tom);
    }

    /** Oppretter en default fagsak, med startdato fra i dag. */
    @Deprecated(forRemoval = true)
    public static Fagsak opprettNy(FagsakYtelseType ytelseType, AktørId bruker) {
        return new Fagsak(ytelseType, bruker, null, null, null, LocalDate.now(), null);
    }

    /** Oppretter en default fagsak, med startdato fra i dag. */
    @Deprecated(forRemoval = true)
    public static Fagsak opprettNy(FagsakYtelseType ytelseType, AktørId bruker, Saksnummer saksnummer) {
        return new Fagsak(ytelseType, bruker, null, null, saksnummer, LocalDate.now(), null);
    }

    public static Fagsak opprettNy(FagsakYtelseType ytelseType, AktørId bruker, Saksnummer saksnummer, LocalDate fom, LocalDate tom) {
        return new Fagsak(ytelseType, bruker, null, null, saksnummer, fom, tom);
    }

    public static Fagsak opprettNy(FagsakYtelseType ytelseType, AktørId bruker, AktørId pleietrengende, AktørId relatertPerson, Saksnummer saksnummer, LocalDate fom, LocalDate tom) {
        return new Fagsak(ytelseType, bruker, pleietrengende, relatertPerson, saksnummer, fom, tom);
    }

    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }

    public Long getId() {
        return id;
    }

    /**
     * @deprecated Kun for test!.
     */
    @Deprecated
    public void setId(Long id) {
        this.id = id;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    void setSaksnummer(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
    }

    public void setPleietrengende(AktørId aktørId) {
        if (aktørId != null && this.pleietrengendeAktørId != null && !this.pleietrengendeAktørId.equals(aktørId)) {
            throw new IllegalArgumentException("Kan ikke oppdatere pleietrengende til en annen person. Prøver å endre fra " + this.pleietrengendeAktørId + " til " + aktørId);
        }
        if (aktørId == null && this.pleietrengendeAktørId != null) {
            throw new IllegalArgumentException("Kan ikke nullstille pleietrengende. Prøver å endre fra " + this.pleietrengendeAktørId + " til " + aktørId);
        }
        this.pleietrengendeAktørId = aktørId;
    }

    public void setRelatertPerson(AktørId aktørId) {
        if (aktørId != null && this.relatertPersonAktørId != null && !this.relatertPersonAktørId.equals(aktørId)) {
            throw new IllegalArgumentException("Kan ikke oppdatere relatertPerson til en annen person. Prøver å endre fra " + this.relatertPersonAktørId + " til " + aktørId);
        }
        if (aktørId == null && this.relatertPersonAktørId != null) {
            throw new IllegalArgumentException("Kan ikke nullstille relatertPerson. Prøver å endre fra " + this.relatertPersonAktørId + " til " + aktørId);
        }
        this.relatertPersonAktørId = aktørId;
    }

    public AktørId getBrukerAktørId() {
        return brukerAktørId;
    }

    public AktørId getPleietrengendeAktørId() {
        return pleietrengendeAktørId;
    }

    public AktørId getRelatertPersonAktørId() {
        return relatertPersonAktørId;
    }

    public boolean erÅpen() {
        return !getFagsakStatus().equals(FagsakStatus.AVSLUTTET);
    }

    public FagsakStatus getStatus() {
        return getFagsakStatus();
    }

    public void setAvsluttet() {
        oppdaterStatus(FagsakStatus.AVSLUTTET);
    }

    void oppdaterStatus(FagsakStatus status) {
        this.setFagsakStatus(status);
    }

    public FagsakYtelseType getYtelseType() {
        return ytelseType;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof Fagsak)) {
            return false;
        }
        Fagsak fagsak = (Fagsak) object;
        return Objects.equals(saksnummer, fagsak.saksnummer)
            && Objects.equals(ytelseType, fagsak.ytelseType)
            && Objects.equals(brukerAktørId, fagsak.brukerAktørId)
            && Objects.equals(getYtelseType(), fagsak.getYtelseType());
    }

    @Override
    public String toString() {
        // tar ikke med brukerAktørId, pleietrengendeAktørId så ikke lekker sensitive opplysninger i logger
        return getClass().getSimpleName() + "<"
            + (id == null ? "" : "fagsakId=" + id + ",")
            + ", saksnummer=" + saksnummer
            + ", ytelseType" + ytelseType
            + ", periode=" + periode
            + ", skalTilInfotrygd=" + skalTilInfotrygd
            + ">";
    }

    @Override
    public int hashCode() {
        return Objects.hash(ytelseType, brukerAktørId);
    }

    public AktørId getAktørId() {
        return brukerAktørId;
    }

    private FagsakStatus getFagsakStatus() {
        return fagsakStatus;
    }

    private void setFagsakStatus(FagsakStatus fagsakStatus) {
        this.fagsakStatus = fagsakStatus;
    }

    public boolean getSkalTilInfotrygd() {
        return skalTilInfotrygd;
    }

    public void setSkalTilInfotrygd(boolean tilInfotrygd) {
        this.skalTilInfotrygd = tilInfotrygd;
    }

    public long getVersjon() {
        return versjon;
    }

    @PreRemove
    protected void onDelete() {
        // FIXME: FPFEIL-2799 (FrodeC): Fjern denne når FPFEIL-2799 er godkjent
        throw new IllegalStateException("Skal aldri kunne slette fagsak. [id=" + id + ", status=" + getFagsakStatus() + ", type=" + ytelseType + "]");
    }

    void setPeriode(LocalDate fom, LocalDate tom) {
        if ((fom == null || fom.equals(Tid.TIDENES_BEGYNNELSE))) {
            throw new IllegalArgumentException(String.format("Alle saker må angi en startdato: [%s, %s]", fom, tom));
        }
        this.periode = DatoIntervallEntitet.fra(fom, tom).toRange();
    }
}
