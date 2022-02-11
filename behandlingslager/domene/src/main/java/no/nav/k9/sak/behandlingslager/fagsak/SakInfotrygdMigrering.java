package no.nav.k9.sak.behandlingslager.fagsak;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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

    @Column(name = "aktiv")
    private Boolean aktiv;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public SakInfotrygdMigrering() {
    }

    public SakInfotrygdMigrering(Long fagsakId, LocalDate skjæringstidspunkt) {
        this.fagsakId = fagsakId;
        this.skjæringstidspunkt = skjæringstidspunkt;
        this.aktiv = true;
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

    public Boolean getAktiv() {
        return aktiv;
    }

    public void setSkjæringstidspunkt(LocalDate skjæringstidspunkt) {
        this.skjæringstidspunkt = skjæringstidspunkt;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }
}
