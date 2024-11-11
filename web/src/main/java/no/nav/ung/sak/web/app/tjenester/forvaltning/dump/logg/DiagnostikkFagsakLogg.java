package no.nav.ung.sak.web.app.tjenester.forvaltning.dump.logg;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreRemove;
import jakarta.persistence.Table;

import no.nav.ung.sak.behandlingslager.BaseEntitet;

@Entity(name = "DiagnostikkFagsakLogg")
@Table(name = "DIAGNOSTIKK_FAGSAK_LOGG")
public class DiagnostikkFagsakLogg extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_DIAGNOSTIKK_FAGSAK_LOGG")
    @Column(name = "id")
    private Long id;

    @Column(name = "fagsak_id", nullable = false, updatable = false, insertable = true)
    private Long fagsakId;

    @Column(name = "begrunnelse", updatable = false, length = 4000)
    private String begrunnelse;

    @Column(name = "tjeneste", updatable = false, length = 200)
    private String tjeneste;

    DiagnostikkFagsakLogg() {
        // Hibernate
    }

    public DiagnostikkFagsakLogg(Long fagsakId, String tjeneste, String begrunnelse) {
        this.fagsakId = fagsakId;
        this.tjeneste = tjeneste;
        this.begrunnelse = begrunnelse;
    }

    public Long getId() {
        return id;
    }

    public Long getFagsakId() {
        return fagsakId;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<fagsakId=" + fagsakId + ">";
    }

    @PreRemove
    protected void onDelete() {
        throw new IllegalStateException("Skal aldri kunne slette fagsak. [id=" + id + "]");
    }
}
