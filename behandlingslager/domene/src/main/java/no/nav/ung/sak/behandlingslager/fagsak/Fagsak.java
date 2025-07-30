package no.nav.ung.sak.behandlingslager.fagsak;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.ung.sak.behandlingslager.aktør.AktørIdConverter;
import no.nav.ung.sak.behandlingslager.kodeverk.BehandlingStatusKodeverdiConverter;
import org.hibernate.annotations.Type;

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
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.PostgreSQLRangeType;
import no.nav.ung.sak.behandlingslager.Range;
import no.nav.ung.sak.behandlingslager.kodeverk.FagsakStatusKodeverdiConverter;
import no.nav.ung.sak.behandlingslager.kodeverk.FagsakYtelseTypeKodeverdiConverter;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

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

    @Convert(converter = AktørIdConverter.class, attributeName = "brukerAktørId")
    //@Column(name = "bruker_aktoer_id",  nullable = false, updatable = false, unique = true)
    @AttributeOverrides(@AttributeOverride(name = "aktørId", column = @Column(name = "bruker_aktoer_id", unique = true, nullable = false, updatable = false)))
    private AktørId brukerAktørId;

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

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Fagsak() {
        // Hibernate
    }

    public Fagsak(FagsakYtelseType ytelseType, AktørId søker, Saksnummer saksnummer, LocalDate fom, LocalDate tom) {
        Objects.requireNonNull(ytelseType, "ytelseType");
        this.ytelseType = ytelseType;
        this.brukerAktørId = søker;
        if (saksnummer != null) {
            setSaksnummer(saksnummer);
        }
        setPeriode(fom, tom);
    }

    /**
     * Oppretter en default fagsak, med startdato fra i dag.
     */
    @Deprecated(forRemoval = true)
    public static Fagsak opprettNy(FagsakYtelseType ytelseType, AktørId bruker) {
        return new Fagsak(ytelseType, bruker, null, LocalDate.now(), null);
    }

    /**
     * Oppretter en default fagsak, med startdato fra i dag.
     */
    @Deprecated(forRemoval = true)
    public static Fagsak opprettNy(FagsakYtelseType ytelseType, AktørId bruker, Saksnummer saksnummer) {
        return new Fagsak(ytelseType, bruker, saksnummer, LocalDate.now(), null);
    }

    public static Fagsak opprettNy(FagsakYtelseType ytelseType, AktørId bruker, Saksnummer saksnummer, LocalDate fom, LocalDate tom) {
        return new Fagsak(ytelseType, bruker, saksnummer, fom, tom);
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


    public AktørId getBrukerAktørId() {
        return brukerAktørId;
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

    public void oppdaterStatus(FagsakStatus status) {
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
        // tar ikke med brukerAktørId så ikke lekker sensitive opplysninger i logger
        return getClass().getSimpleName() + "<"
            + (id == null ? "" : "fagsakId=" + id + ",")
            + ", saksnummer=" + saksnummer
            + ", ytelseType" + ytelseType
            + ", periode=" + periode
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
