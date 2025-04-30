package no.nav.ung.sak.behandlingslager.formidling;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.slf4j.Logger;

@Entity(name = "VedtaksbrevValgEntitet")
@Table(name = "vedtaksbrev_valg")
public class VedtaksbrevValgEntitet extends BaseEntitet {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(VedtaksbrevValgEntitet.class);

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

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    VedtaksbrevValgEntitet() {}

    public VedtaksbrevValgEntitet(Long behandlingId, boolean redigert, boolean hindret, String redigertBrevHtml) {
        this.behandlingId = behandlingId;
        this.redigert = redigert;
        this.hindret = hindret;
        this.redigertBrevHtml = sanitizeHtml(redigertBrevHtml);

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

    public void rensOgSettRedigertHtml(String redigertBrevHtml) {
        if ((redigertBrevHtml == null || redigertBrevHtml.isBlank()) && this.redigertBrevHtml != null && !this.redigertBrevHtml.isBlank()) {
            LOG.info("Fjerner redigert brev html!");
        }

        this.redigertBrevHtml = sanitizeHtml(redigertBrevHtml);
    }

    private static String sanitizeHtml(String redigertBrevHtml) {
        if (redigertBrevHtml == null) {
            return null;
        }
        return new XhtmlBrevRenser().rens(redigertBrevHtml);
    }

    public void tilbakestillVedTilbakehopp() {
        //Fjerner ikke redigert tekst i tilfelle saksbehandler ønsker å bruke den
        setRedigert(false);
        setHindret(false);
    }
}
