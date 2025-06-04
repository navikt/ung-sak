package no.nav.ung.sak.behandlingslager.perioder;

import java.util.*;

import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
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

    /** Henter ut første perioden i grunnlaget, dersom det finnes perioder.
     * Dersom grunnlaget ikke har noen perioder eller det finnes flere perioder vil den kaste en IllegalStateException.
     * <p>
     * Enn så lenge er det kun tillatt med én periode i grunnlaget, og denne metoden er laget for å fasilitere uthenting av kun en periode.
     * Løsningen bør i størst mulig grad tilpasses for å håndtere flere perioder, men der man skal anta at det er kun én periode i grunnlaget burde denne metoden brukes.
     * @throws IllegalStateException dersom grunnlaget ikke har noen perioder eller det finnes flere perioder.
     * @return Første perioden i grunnlaget, dersom det finnes perioder.
     */
    public DatoIntervallEntitet hentForEksaktEnPeriode() {
        var perioder = ungdomsprogramPerioder.getPerioder();
        if (perioder.isEmpty()) {
            throw new IllegalStateException("Grunnlaget har ingen perioder");
        }
        if (perioder.size() > 1) {
            throw new IllegalStateException("Grunnlaget har flere perioder, forventet kun én");
        }
        return perioder.iterator().next().getPeriode();
    }


    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof UngdomsprogramPeriodeGrunnlag that)) return false;
        return Objects.equals(ungdomsprogramPerioder, that.ungdomsprogramPerioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ungdomsprogramPerioder);
    }
}
