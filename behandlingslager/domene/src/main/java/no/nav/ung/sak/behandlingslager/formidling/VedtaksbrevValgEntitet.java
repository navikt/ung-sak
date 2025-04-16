package no.nav.ung.sak.behandlingslager.formidling;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

@Entity(name = "VedtaksbrevValgEntitet")
@Table(name = "vedtaksbrev_valg")
public class VedtaksbrevValgEntitet extends BaseEntitet {

    @Id
    @SequenceGenerator(name = "seq_vedtaksbrev_valg", sequenceName = "seq_vedtaksbrev_valg")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_vedtaksbrev_valg")
    private Long id;

    @Column(name = "behandling_id", updatable = false, nullable = false)
    private Long behandlingId;

    @Column(name = "redigert", nullable = false)
    private boolean redigert;

    @Column(name = "hindret", nullable = false)
    private boolean hindret;

    @Column(name = "redigert_brev_html")
    private String redigertBrevHtml;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VedtaksbrevValgEntitet() {}

    public VedtaksbrevValgEntitet(Long behandlingId, boolean redigert, boolean hindret, String redigertBrevHtml) {
        this.behandlingId = behandlingId;
        this.redigert = redigert;
        this.hindret = hindret;
        this.redigertBrevHtml = redigertBrevHtml;

    }

    public static VedtaksbrevValgEntitet ny(Long behandlingId) {
        return new VedtaksbrevValgEntitet(
            behandlingId,
            false,
            false,
            null
        );
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public boolean isRedigert() {
        return redigert;
    }

    public boolean isHindret() {
        return hindret;
    }

    public String getRedigertBrevHtml() {
        return redigertBrevHtml;
    }

    public long getVersjon() {
        return versjon;
    }

    public void setRedigert(boolean redigert) {
        this.redigert = redigert;
    }

    public void setHindret(boolean hindret) {
        this.hindret = hindret;
    }

    public void setRedigertBrevHtml(String redigertBrevHtml) {
        this.redigertBrevHtml = redigertBrevHtml;
    }
}
