package no.nav.k9.sak.behandlingslager.fagsak;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.diff.ChangeTracked;

@Entity(name = "SakInfotrygdMigrering")
@Table(name = "SAK_INFOTRYGD_MIGRERING")
public class SakInfotrygdMigrering extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAK_INFOTRYGD_MIGRERING")
    private Long id;

    @ChangeTracked
    @Column(name = "fagsak_id", nullable = false, updatable = false)
    private Long fagsakId;

    @Column(name = "skjaeringstidspunkt", nullable = false)
    private LocalDate skjæringstidspunkt;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public SakInfotrygdMigrering() {
    }

    public SakInfotrygdMigrering(Long fagsakId, LocalDate skjæringstidspunkt) {
        this.fagsakId = fagsakId;
        this.skjæringstidspunkt = skjæringstidspunkt;
    }

    public Long getId() {
        return id;
    }


    public Long getFagsakId() {
        return fagsakId;
    }

    public LocalDate getSkjæringstidspunkt() {
        return skjæringstidspunkt;
    }
}
