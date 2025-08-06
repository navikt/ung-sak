package no.nav.ung.sak.behandlingslager.formidling.bestilling;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

import java.util.UUID;

@Entity(name = "BrevbestillingEntitet")
@Table(name = "brevbestilling")
public class BrevbestillingEntitet extends BaseEntitet {
    @Id
    @SequenceGenerator(name = "seq_brevbestilling", sequenceName = "seq_brevbestilling")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_brevbestilling")
    private Long id;

    @Column(name = "brevbestilling_uuid", updatable = false, nullable = false)
    private UUID brevbestillingUuid;


    @Column(name = "fagsak_id", updatable = false, nullable = false)
    private Long fagsakId;

    @Column(name = "behandling_id", updatable = false, nullable = false)
    private Long behandlingId;

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
     * journalpost fra dokarkiv etter journalføring
     */
    @Column(name = "journalpost_id")
    private String journalpostId;

    /**
     * id fra dokdist etter distribusjon
     */
    @Column(name = "dokdist_bestilling_id")
    private String dokdistBestillingId;

    @Column(name = "vedtaksbrev", updatable = false, nullable = false)
    private boolean vedtaksbrev;

    @Embedded
    private BrevMottaker mottaker;

    @Column(name = "aktiv", nullable = false, updatable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;


    public BrevbestillingEntitet(Long fagsakId, Long behandlingId, DokumentMalType dokumentMalType, TemplateType templateType, BrevbestillingStatusType status, BrevMottaker mottaker) {
        this.brevbestillingUuid = UUID.randomUUID();
        this.fagsakId = fagsakId;
        this.behandlingId = behandlingId;
        this.dokumentMalType = dokumentMalType;
        this.templateType = templateType;
        this.status = status;
        this.mottaker = mottaker;
        this.vedtaksbrev = dokumentMalType.isVedtaksbrevmal();
    }

    public BrevbestillingEntitet(Long fagsakId, Long behandlingId, DokumentMalType dokumentMalType, BrevbestillingStatusType status) {
        this.brevbestillingUuid = UUID.randomUUID();
        this.fagsakId = fagsakId;
        this.behandlingId = behandlingId;
        this.dokumentMalType = dokumentMalType;
        this.status = status;
        this.vedtaksbrev = dokumentMalType.isVedtaksbrevmal();
    }

    public BrevbestillingEntitet() {
    }

    public static BrevbestillingEntitet nyBrevbestilling(Long fagsakId, Long behandlingId, DokumentMalType dokumentMalType) {
        return new BrevbestillingEntitet(fagsakId, behandlingId, dokumentMalType, BrevbestillingStatusType.NY);
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

    public Long getBehandlingId() {
        return behandlingId;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    public boolean isVedtaksbrev() {
        return vedtaksbrev;
    }

    public void journalført(String journalpostId, TemplateType templateType, BrevMottaker brevMottaker) {
        this.journalpostId = journalpostId;
        this.templateType = templateType;
        this.mottaker = brevMottaker;
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
            ", fagsakId=" + fagsakId +
            ", behandlingId=" + behandlingId +
            ", dokumentMalType=" + dokumentMalType +
            ", templateType=" + templateType +
            ", status=" + status +
            ", journalpostId='" + journalpostId + '\'' +
            ", dokdistBestillingId='" + dokdistBestillingId + '\'' +
            ", vedtaksbrev=" + vedtaksbrev +
            ", mottakerType=" + mottaker.getMottakerIdType() +
            ", versjon=" + versjon +
            '}';
    }
}
