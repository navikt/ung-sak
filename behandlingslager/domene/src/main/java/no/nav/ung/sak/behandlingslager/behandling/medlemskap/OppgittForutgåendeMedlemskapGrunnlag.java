package no.nav.ung.sak.behandlingslager.behandling.medlemskap;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

import java.util.Objects;
import java.util.Set;

@Entity(name = "OppgittForutgåendeMedlemskapGrunnlag")
@Table(name = "GR_OPPGITT_FMEDLEMSKAP")
public class OppgittForutgåendeMedlemskapGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_OPPGITT_FMEDLEMSKAP")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false)
    private Long behandlingId;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "oppgitt_fmedlemskap_holder_id", nullable = false, updatable = false)
    private OppgittForutgåendeMedlemskapHolder holder;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public OppgittForutgåendeMedlemskapGrunnlag() {
    }

    public OppgittForutgåendeMedlemskapGrunnlag(Long behandlingId, OppgittForutgåendeMedlemskapHolder holder) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        Objects.requireNonNull(holder, "holder");
        this.behandlingId = behandlingId;
        this.holder = holder;
    }

    public Long getId() {
        return id;
    }

    public Long getBehandlingId() {
        return behandlingId;
    }

    public OppgittForutgåendeMedlemskapHolder getHolder() {
        return holder;
    }

    public Set<OppgittForutgåendeMedlemskapPeriode> getOppgittePerioder() {
        return holder.getPerioder();
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void deaktiver() {
        this.aktiv = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OppgittForutgåendeMedlemskapGrunnlag that = (OppgittForutgåendeMedlemskapGrunnlag) o;
        return Objects.equals(holder, that.holder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(holder);
    }

    @Override
    public String toString() {
        return "OppgittForutgåendeMedlemskapGrunnlag{" +
            "behandlingId=" + behandlingId +
            ", holder=" + holder +
            ", aktiv=" + aktiv +
            '}';
    }
}
