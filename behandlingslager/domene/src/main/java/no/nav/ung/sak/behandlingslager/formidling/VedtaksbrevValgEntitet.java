package no.nav.ung.sak.behandlingslager.formidling;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.DokumentMalTypeKodeverdiConverter;
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

    @Column(name = "dokumentmal_type", updatable = false, nullable = false)
    @Convert(converter = DokumentMalTypeKodeverdiConverter.class)
    private DokumentMalType dokumentMalType;

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

    public VedtaksbrevValgEntitet(Long behandlingId, DokumentMalType dokumentMalType, boolean redigert, boolean hindret, String redigertBrevHtml) {
        this.behandlingId = behandlingId;
        this.dokumentMalType = dokumentMalType;
        this.redigert = redigert;
        this.hindret = hindret;
        this.redigertBrevHtml = sanitizeHtml(redigertBrevHtml);

    }

    public static VedtaksbrevValgEntitet ny(Long behandlingId, DokumentMalType dokumentMalType) {
        return new VedtaksbrevValgEntitet(
            behandlingId,
            dokumentMalType,
            false,
            false,
            null);
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public DokumentMalType getDokumentMalType() {
        return dokumentMalType;
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

    @Override
    public String toString() {
        return "VedtaksbrevValgEntitet{" +
            "id=" + id +
            ", behandlingId=" + behandlingId +
            ", redigert=" + redigert +
            ", hindret=" + hindret +
            ", redigertBrevHtml='" + (redigertBrevHtml != null) + '\'' +
            ", versjon=" + versjon +
            '}';
    }
}
