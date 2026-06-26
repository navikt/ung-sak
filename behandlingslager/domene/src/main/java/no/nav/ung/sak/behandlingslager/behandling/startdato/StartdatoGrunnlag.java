package no.nav.ung.sak.behandlingslager.behandling.startdato;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import org.hibernate.annotations.Immutable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

@Entity(name = "StartdatoGrunnlag")
@Table(name = "GR_STARTDATO")
public class StartdatoGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_GR_STARTDATO")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    /**
     * Søknadsperioder som er relevant og som har krav som skal vurderes i behandlingen
     */
    @ManyToOne
    @Immutable
    @JoinColumn(name = "relevante_startdatoer_id", nullable = true, updatable = false, unique = true)
    private Startdatoer relevanteStartdatoer;

    /**
     * Alle søknadsperioder med krav som har kommet inn i denne og tidligere behandlinger
     */
    @ManyToOne
    @Immutable
    @JoinColumn(name = "oppgitte_startdatoer_id", nullable = false, updatable = false, unique = true)
    private Startdatoer oppgitteStartdatoer;


    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public StartdatoGrunnlag() {
    }

    StartdatoGrunnlag(Long behandlingId, StartdatoGrunnlag grunnlag) {
        this.behandlingId = behandlingId;
        this.oppgitteStartdatoer = grunnlag.oppgitteStartdatoer;
        this.relevanteStartdatoer = grunnlag.relevanteStartdatoer;
    }

    public StartdatoGrunnlag(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public Long getId() {
        return id;
    }

    public Startdatoer getOppgitteStartdatoer() {
        return oppgitteStartdatoer;
    }

    public Startdatoer getRelevanteStartdatoer() {
        return relevanteStartdatoer;
    }


    public boolean isAktiv() {
        return aktiv;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    void leggTil(Collection<SøktStartdato> startdatoer) {
        if (id != null) {
            throw new IllegalStateException("[Utvikler feil] Kan ikke editere persistert grunnlag");
        }
        var perioder = this.oppgitteStartdatoer != null ? new HashSet<>(this.oppgitteStartdatoer.getStartdatoer()) : new HashSet<>(startdatoer);
        perioder.addAll(startdatoer);
        this.oppgitteStartdatoer = new Startdatoer(perioder);
    }

    void setRelevanteStartdatoer(Startdatoer relevanteSøknader) {
        Objects.requireNonNull(relevanteSøknader);
        this.relevanteStartdatoer = relevanteSøknader;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StartdatoGrunnlag that = (StartdatoGrunnlag) o;
        return Objects.equals(oppgitteStartdatoer, that.oppgitteStartdatoer)
            && Objects.equals(relevanteStartdatoer, that.relevanteStartdatoer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgitteStartdatoer, relevanteStartdatoer);
    }
}
