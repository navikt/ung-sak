package no.nav.ung.sak.formidling.domene;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.formidling.template.TemplateType;

@Entity(name = "BrevbestillingEntitet")
@Table(name = "brevbestilling")
public class BrevbestillingEntitet extends BaseEntitet {
    @Id
    @SequenceGenerator(name = "seq_brevbestilling", sequenceName = "seq_brevbestilling")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_brevbestilling")
    private Long id;

    @Column(name = "brevbestilling_uuid", updatable = false, nullable = false)
    private UUID brevbestillingUuid;

    /**
     * saksnummer bestillingen journalføres på, kan være GENERELL_SAK
     */
    @Column(name = "saksnummer", updatable = false, nullable = false)
    private String saksnummer;

    /**
     * malen bestilt
     */
    @Column(name = "dokumentmal_type", updatable = false, nullable = false)
    @Convert(converter = DokumentMalTypeKodeverdiConverter.class)
    private DokumentMalType dokumentMalType;

    /**
     * template brukt til å lage brevet
     */
    @Column(name = "template_type")
    @Enumerated(EnumType.STRING)
    private TemplateType templateType;

    /**
     * status for å gjøre journalføring og distribuering i to operasjoner
     */
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private BrevbestillingStatusType status;

    /**
     * template data som ikke er utledet, f.eks. fritekst
     */
    @Column(name = "dokumentdata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON) //TODO endre til å bruke json subtypes?
    private JsonNode dokumentdata;

    /**
     * journalpost fra dokarkiv etter journalføring
     */
    @Column(name = "journalpost_id")
    private String journalpostId;

    /**
     * id fra dokdist etter distribusjon
     */
    @Column(name = "dokdist_bestilling_id")
    private String dokdistBestillingId;

    @Embedded
    private BrevMottaker mottaker;


    public BrevbestillingEntitet(String saksnummer, DokumentMalType dokumentMalType, BrevbestillingStatusType status, JsonNode dokumentdata, BrevMottaker mottaker) {
        this.brevbestillingUuid = UUID.randomUUID();
        this.saksnummer = saksnummer;
        this.dokumentMalType = dokumentMalType;
        this.status = status;
        this.dokumentdata = dokumentdata;
        this.mottaker = mottaker;
    }

    public BrevbestillingEntitet() {
    }

    public UUID getBrevbestillingUuid() {
        return brevbestillingUuid;
    }

    public BrevbestillingStatusType getStatus() {
        return status;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public String getDokdistBestillingId() {
        return dokdistBestillingId;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void generertOgJournalført(TemplateType templateType, String journalpostId) {
        this.journalpostId = journalpostId;
        this.templateType = templateType;
        status = BrevbestillingStatusType.JOURNALFØRT;
    }

    public void fullført(String dokdistBestillingId) {
        this.dokdistBestillingId = dokdistBestillingId;
        status = BrevbestillingStatusType.FULLFØRT;
    }

    public DokumentMalType getDokumentMalType() {
        return dokumentMalType;
    }

    public TemplateType getTemplateType() {
        return templateType;
    }

    public JsonNode getDokumentData() {
        return dokumentdata;
    }

    public BrevMottaker getMottaker() {
        return mottaker;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "BrevbestillingEntitet{" +
               "id=" + id +
               ", brevbestillingUuid=" + brevbestillingUuid +
               ", saksnummer='" + saksnummer + '\'' +
               ", templateType=" + templateType +
               ", dokumentMalType=" + dokumentMalType +
               ", status=" + status +
               ", journalpostId='" + journalpostId + '\'' +
               ", dokdistBestillingId='" + dokdistBestillingId + '\'' +
               ", mottakerType=" + mottaker.getMottakerIdType() +
               '}';
    }
}
