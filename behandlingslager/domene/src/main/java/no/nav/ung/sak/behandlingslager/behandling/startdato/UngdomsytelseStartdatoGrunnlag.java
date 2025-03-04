package no.nav.ung.sak.behandlingslager.behandling.startdato;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Immutable;

import no.nav.ung.sak.behandlingslager.BaseEntitet;

@Entity(name = "UngdomsytelseStartdatoGrunnlag")
@Table(name = "UNG_GR_STARTDATO")
public class UngdomsytelseStartdatoGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_GR_STARTDATO")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    /**
     * Søknadsperioder som er relevant og som har krav som skal vurderes i behandlingen
     */
    @ManyToOne
    @Immutable
    @JoinColumn(name = "relevante_startdatoer_id", nullable = true, updatable = false, unique = true)
    private UngdomsytelseStartdatoer relevanteStartdatoer;

    /**
     * Alle søknadsperioder med krav som har kommet inn i denne og tidligere behandlinger
     */
    @ManyToOne
    @Immutable
    @JoinColumn(name = "oppgitte_startdatoer_id", nullable = false, updatable = false, unique = true)
    private UngdomsytelseStartdatoer oppgitteStartdatoer;


    @ChangeTracked
    @BatchSize(size = 20)
    @JoinColumn(name = "UNG_GR_STARTDATO_ID", nullable = false)
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REFRESH})
    private Set<UngdomsytelseBekreftetPeriodeEndring> bekreftetPeriodeEndringer;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UngdomsytelseStartdatoGrunnlag() {
    }

    UngdomsytelseStartdatoGrunnlag(Long behandlingId, UngdomsytelseStartdatoGrunnlag grunnlag) {
        this.behandlingId = behandlingId;
        this.oppgitteStartdatoer = grunnlag.oppgitteStartdatoer;
        this.relevanteStartdatoer = grunnlag.relevanteStartdatoer;
    }

    public UngdomsytelseStartdatoGrunnlag(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public Long getId() {
        return id;
    }

    public UngdomsytelseStartdatoer getOppgitteStartdatoer() {
        return oppgitteStartdatoer;
    }

    public UngdomsytelseStartdatoer getRelevanteStartdatoer() {
        return relevanteStartdatoer;
    }

    public Set<UngdomsytelseBekreftetPeriodeEndring> getBekreftetPeriodeEndringer() {
        return bekreftetPeriodeEndringer;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    void leggTil(Collection<UngdomsytelseSøktStartdato> startdatoer) {
        if (id != null) {
            throw new IllegalStateException("[Utvikler feil] Kan ikke editere persistert grunnlag");
        }
        var perioder = this.oppgitteStartdatoer != null ? new HashSet<>(this.oppgitteStartdatoer.getStartdatoer()) : new HashSet<>(startdatoer);
        perioder.addAll(startdatoer);
        this.oppgitteStartdatoer = new UngdomsytelseStartdatoer(perioder);
    }

    void leggTil(UngdomsytelseBekreftetPeriodeEndring bekreftetPeriodeEndring) {
        if (id != null) {
            throw new IllegalStateException("[Utvikler feil] Kan ikke editere persistert grunnlag");
        }
        Set<UngdomsytelseBekreftetPeriodeEndring> perioder = this.bekreftetPeriodeEndringer != null ? new HashSet<>(this.bekreftetPeriodeEndringer) : new HashSet<>();
        perioder.add(bekreftetPeriodeEndring);
        this.bekreftetPeriodeEndringer = perioder;
    }

    void setRelevanteStartdatoer(UngdomsytelseStartdatoer relevanteSøknader) {
        Objects.requireNonNull(relevanteSøknader);
        this.relevanteStartdatoer = relevanteSøknader;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UngdomsytelseStartdatoGrunnlag that = (UngdomsytelseStartdatoGrunnlag) o;
        return Objects.equals(oppgitteStartdatoer, that.oppgitteStartdatoer)
            && Objects.equals(relevanteStartdatoer, that.relevanteStartdatoer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgitteStartdatoer, relevanteStartdatoer);
    }
}
