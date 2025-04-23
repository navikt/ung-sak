package no.nav.ung.sak.behandlingslager.formidling.bestilling;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

@Entity(name = "BehandlingBrevbestillingEntitet")
@Table(name = "brevbestilling_behandling")
public class BehandlingBrevbestillingEntitet extends BaseEntitet {

    @Id
    @SequenceGenerator(name = "seq_behandling_brevbestilling", sequenceName = "seq_behandling_brevbestilling")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_behandling_brevbestilling")
    private Long id;

    @Column(name = "behandling_id", updatable = false, nullable = false)
    private Long behandlingId;

    @Column(name = "vedtaksbrev", updatable = false, nullable = false)
    private boolean vedtaksbrev;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "brevbestilling_id", nullable = false)
    private BrevbestillingEntitet brevbestillingEntitet;

    @Column(name = "aktiv", nullable = false, updatable = false)
    private boolean aktiv = true;


    public BehandlingBrevbestillingEntitet(Long behandlingId, boolean vedtaksbrev, BrevbestillingEntitet brevbestillingEntitet) {
        this.behandlingId = behandlingId;
        this.vedtaksbrev = vedtaksbrev;
        this.brevbestillingEntitet = brevbestillingEntitet;
    }

    public BehandlingBrevbestillingEntitet() {
    }

    public BrevbestillingEntitet getBestilling() {
        return brevbestillingEntitet;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public boolean isVedtaksbrev() {
        return vedtaksbrev;
    }

    @Override
    public String toString() {
        return "BehandlingBrevbestillingEntitet{" +
               "id=" + id +
               ", behandlingId=" + behandlingId +
               ", vedtaksbrev=" + vedtaksbrev +
               ", brevbestillingEntitet=" + brevbestillingEntitet +
               '}';
    }
}
