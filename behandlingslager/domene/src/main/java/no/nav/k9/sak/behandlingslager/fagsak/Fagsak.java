package no.nav.k9.sak.behandlingslager.fagsak;

import java.time.LocalDate;
import java.util.Objects;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PreRemove;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.k9.kodeverk.behandling.BehandlingTema;
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

    @Convert(converter = FagsakStatusKodeverdiConverter.class)
    @Column(name = "fagsak_status", nullable = false)
    private FagsakStatus fagsakStatus = FagsakStatus.DEFAULT;

    /**
     * Offisielt tildelt saksnummer for GSAK.
     */
    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "saksnummer", column = @Column(name = "saksnummer")))
    private Saksnummer saksnummer;
    
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fomDato", column = @Column(name = "gjelder_fom")),
            @AttributeOverride(name = "tomDato", column = @Column(name = "gjelder_tom"))
    })
    private DatoIntervallEntitet periode= DatoIntervallEntitet.fraOgMed(LocalDate.now());

    @Column(name = "til_infotrygd", nullable = false)
    private boolean skalTilInfotrygd = false;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    Fagsak() {
        // Hibernate
    }

    private Fagsak(FagsakYtelseType ytelseType, AktørId søker) {
        this(ytelseType, søker, null);
    }

    public Fagsak(FagsakYtelseType ytelseType, AktørId søker, Saksnummer saksnummer) {
        Objects.requireNonNull(ytelseType, "ytelseType");
        this.ytelseType = ytelseType;
        this.brukerAktørId = søker;
        if (saksnummer != null) {
            setSaksnummer(saksnummer);
        }
    }

    public Fagsak(FagsakYtelseType ytelseType, AktørId bruker, AktørId pleietrengende, Saksnummer saksnummer) {
        this(ytelseType, bruker, saksnummer);
        this.pleietrengendeAktørId = pleietrengende;
    }

    public static Fagsak opprettNy(FagsakYtelseType ytelseType, AktørId bruker) {
        return new Fagsak(ytelseType, bruker);
    }

    public static Fagsak opprettNy(FagsakYtelseType ytelseType, AktørId bruker, Saksnummer saksnummer) {
        return new Fagsak(ytelseType, bruker, saksnummer);
    }

    public static Fagsak opprettNy(FagsakYtelseType ytelseType, AktørId bruker, AktørId pleietrengende, Saksnummer saksnummer) {
        return new Fagsak(ytelseType, bruker, pleietrengende, saksnummer);
    }

    public static BehandlingTema fraFagsakHendelse(FagsakYtelseType ytelseType) {
        // FIXME K9 kodeverk/logikk
        if (FagsakYtelseType.PLEIEPENGER_SYKT_BARN.equals(ytelseType)) {
            return BehandlingTema.PLEIEPENGER_SYKT_BARN;
        }
        return BehandlingTema.UDEFINERT;
    }
    
    public DatoIntervallEntitet getPeriode() {
        return periode;
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

    public void setPleietrengende(AktørId pleietrengendeAktørId) {
        if (pleietrengendeAktørId != null && this.pleietrengendeAktørId != null && !this.pleietrengendeAktørId.equals(pleietrengendeAktørId)) {
            throw new IllegalArgumentException("Kan ikke oppdatere pleietrengende til en annen person. Prøver å endre fra " + this.pleietrengendeAktørId + " til " + pleietrengendeAktørId);
        }
        if (pleietrengendeAktørId == null && this.pleietrengendeAktørId != null) {
            throw new IllegalArgumentException("Kan ikke nullstille pleietrengende. Prøver å endre fra " + this.pleietrengendeAktørId + " til " + pleietrengendeAktørId);
        }
        this.pleietrengendeAktørId = pleietrengendeAktørId;
    }

    public AktørId getBrukerAktørId() {
        return brukerAktørId;
    }

    public AktørId getPleietrengendeAktørId() {
        return pleietrengendeAktørId;
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
        return getClass().getSimpleName() + "<" //$NON-NLS-1$
            + (id == null ? "" : "id=" + id + ",") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            + " bruker=" + brukerAktørId //$NON-NLS-1$
            + ">"; //$NON-NLS-1$
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

    public BehandlingTema getBehandlingTema() {
        // FIXME K9 kodeverk/logikk
        return fraFagsakHendelse(this.getYtelseType());
    }
}
