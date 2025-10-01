package no.nav.ung.sak.behandlingslager.formidling.bestilling;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import org.hibernate.annotations.Immutable;

@Entity(name = "BehandlingVedtaksbrev")
@Immutable
@Table(name = "behandling_vedtaksbrev")
public class BehandlingVedtaksbrev extends BaseEntitet {

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

    @Column(name = "beskrivelse", updatable = false)
    private String beskrivelse;

    @OneToOne
    @JoinColumn(name = "brevbestilling_id", updatable = false)
    private BrevbestillingEntitet brevbestilling;

    @OneToOne
    @JoinColumn(name = "vedtaksbrev_valg_id", updatable = false)
    private VedtaksbrevValgEntitet vedtaksbrevValg;


    public static BehandlingVedtaksbrev medBestilling(BrevbestillingEntitet brevBestilling, String beskrivelse, VedtaksbrevResultatType resultatType, VedtaksbrevValgEntitet vedtaksbrevValg) {
        return new BehandlingVedtaksbrev(brevBestilling.getBehandlingId(), brevBestilling.getFagsakId(), resultatType, beskrivelse, brevBestilling, vedtaksbrevValg);
    }

    public static BehandlingVedtaksbrev utenBestilling(Long behandlingId, Long fagsakId, VedtaksbrevResultatType resultatType, String beskrivelse, VedtaksbrevValgEntitet valg) {
        return new BehandlingVedtaksbrev(behandlingId, fagsakId, resultatType, beskrivelse, null, valg);
    }

    // Default constructor required by JPA
    public BehandlingVedtaksbrev() {
    }

    public BehandlingVedtaksbrev(Long behandlingId, Long fagsakId, VedtaksbrevResultatType resultatType, String beskrivelse, BrevbestillingEntitet brevbestilling, VedtaksbrevValgEntitet vedtaksbrevValg) {
        this.behandlingId = behandlingId;
        this.fagsakId = fagsakId;
        this.resultatType = resultatType;
        this.beskrivelse = beskrivelse;
        this.brevbestilling = brevbestilling;
        this.vedtaksbrevValg = vedtaksbrevValg;
    }

    public VedtaksbrevResultatType getResultatType() {
        return resultatType;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public BrevbestillingEntitet getBrevbestilling() {
        return brevbestilling;
    }

    public VedtaksbrevValgEntitet getVedtaksbrevValg() {
        return vedtaksbrevValg;
    }
}
