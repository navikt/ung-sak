package no.nav.ung.sak.behandlingslager.perioder;

import java.util.*;

import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

@Entity(name = "UngdomsprogramPeriodeGrunnlag")
@Table(name = "UNG_GR_UNGDOMSPROGRAMPERIODE")
public class UngdomsprogramPeriodeGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_GR_UNGDOMSPROGRAMPERIODE")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    @ManyToOne
    @Immutable
    @ChangeTracked
    @JoinColumn(name = "ung_ungdomsprogramperioder_id", nullable = false, updatable = false, unique = true)
    private UngdomsprogramPerioder ungdomsprogramPerioder;

    @Column(name = "grunnlagsreferanse", updatable = false, unique = true)
    private UUID grunnlagsreferanse;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;


    public UngdomsprogramPeriodeGrunnlag() {
    }

    UngdomsprogramPeriodeGrunnlag(Long behandlingId, UngdomsprogramPeriodeGrunnlag grunnlag) {
        this.behandlingId = behandlingId;
        this.ungdomsprogramPerioder = grunnlag.ungdomsprogramPerioder;
        this.grunnlagsreferanse = UUID.randomUUID();
    }

    public UngdomsprogramPeriodeGrunnlag(Long behandlingId) {
        this.behandlingId = behandlingId;
        this.grunnlagsreferanse = UUID.randomUUID();
    }

    public Long getId() {
        return id;
    }

    public UUID getGrunnlagsreferanse() {
        return grunnlagsreferanse;
    }

    void leggTil(Collection<UngdomsprogramPeriode> ungdomsprogramPeriode) {
        if (id != null) {
            throw new IllegalStateException("[Utvikler feil] Kan ikke editere persistert grunnlag");
        }
        var perioder = this.ungdomsprogramPerioder != null ? new HashSet<>(this.ungdomsprogramPerioder.getPerioder()) : new HashSet<UngdomsprogramPeriode>(Set.of());
        perioder.addAll(ungdomsprogramPeriode);
        this.ungdomsprogramPerioder = new UngdomsprogramPerioder(perioder);
    }

    public UngdomsprogramPerioder getUngdomsprogramPerioder() {
        return ungdomsprogramPerioder;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UngdomsprogramPeriodeGrunnlag that)) return false;
        return Objects.equals(behandlingId, that.behandlingId) && Objects.equals(ungdomsprogramPerioder, that.ungdomsprogramPerioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(behandlingId, ungdomsprogramPerioder);
    }
}
