package no.nav.ung.sak.behandlingslager.formidling.bestilling;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.Immutable;

@Entity(name = "VedtaksbrevResultatEntitet")
@Immutable
@Table(name = "behandling_vedtaksbrev")
public class VedtaksbrevResultatEntitet extends BaseEntitet {

    @Id
    @SequenceGenerator(name = "seq_behandling_vedtaksbrev", sequenceName = "seq_behandling_vedtaksbrev")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_behandling_vedtaksbrev")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @Column(name = "fagsak_id", nullable = false, updatable = false)
    private Long fagsakId;

    @Column(name = "resultat_type", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private VedtaksbrevResultatType resultatType;

    @Column(name = "forklaring")
    private String forklaring;

    @OneToOne
    @JoinColumn(name = "brevbestilling_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_brevbestilling"))
    private BrevbestillingEntitet brevbestilling;

    public static VedtaksbrevResultatEntitet medBestilling(Long behandlingId, Long fagsakId, VedtaksbrevResultatType resultatType, String forklaring, BrevbestillingEntitet brevBestilling) {
        return new VedtaksbrevResultatEntitet(behandlingId, fagsakId, resultatType, forklaring, brevBestilling);
    }

    public static VedtaksbrevResultatEntitet utenBestilling(Long behandlingId, Long fagsakId, VedtaksbrevResultatType resultatType, String forklaring) {
        return new VedtaksbrevResultatEntitet(behandlingId, fagsakId, resultatType, forklaring, null);
    }

    // Default constructor required by JPA
    public VedtaksbrevResultatEntitet() {
    }

    public VedtaksbrevResultatEntitet(Long behandlingId, Long fagsakId, VedtaksbrevResultatType resultatType, String forklaring, BrevbestillingEntitet brevbestilling) {
        this.behandlingId = behandlingId;
        this.fagsakId = fagsakId;
        this.resultatType = resultatType;
        this.forklaring = forklaring;
        this.brevbestilling = brevbestilling;
    }

    public VedtaksbrevResultatType getResultatType() {
        return resultatType;
    }

    public String getForklaring() {
        return forklaring;
    }

    public BrevbestillingEntitet getBrevbestilling() {
        return brevbestilling;
    }
}
