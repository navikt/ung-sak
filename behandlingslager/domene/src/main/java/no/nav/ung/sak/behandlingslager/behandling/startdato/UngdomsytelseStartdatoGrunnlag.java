package no.nav.ung.sak.behandlingslager.behandling.startdato;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

@Entity(name = "UngdomsytelseSøknadGrunnlag")
@Table(name = "UNG_GR_SOEKNADGRUNNLAG")
public class UngdomsytelseStartdatoGrunnlag extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_GR_SOEKNADGRUNNLAG")
    private Long id;

    @Column(name = "behandling_id", nullable = false, updatable = false, unique = true)
    private Long behandlingId;

    /**
     * Søknadsperioder som er relevant og som har krav som skal vurderes i behandlingen
     */
    @ManyToOne
    @Immutable
    @JoinColumn(name = "relevante_soeknader_id", nullable = true, updatable = false, unique = true)
    private UngdomsytelseStartdatoer relevanteSøknader;

    /**
     * Alle søknadsperioder med krav som har kommet inn i denne og tidligere behandlinger
     */
    @ManyToOne
    @Immutable
    @JoinColumn(name = "oppgitte_soeknader_id", nullable = false, updatable = false, unique = true)
    private UngdomsytelseStartdatoer oppgitteSøknader;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public UngdomsytelseStartdatoGrunnlag() {
    }

    UngdomsytelseStartdatoGrunnlag(Long behandlingId, UngdomsytelseStartdatoGrunnlag grunnlag) {
        this.behandlingId = behandlingId;
        this.oppgitteSøknader = grunnlag.oppgitteSøknader;
        this.relevanteSøknader = grunnlag.relevanteSøknader;
    }

    public UngdomsytelseStartdatoGrunnlag(Long behandlingId) {
        this.behandlingId = behandlingId;
    }

    public Long getId() {
        return id;
    }

    public UngdomsytelseStartdatoer getOppgitteSøknader() {
        return oppgitteSøknader;
    }

    public UngdomsytelseStartdatoer getRelevantSøknader() {
        return relevanteSøknader;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    void leggTil(Collection<UngdomsytelseSøktStartdato> søknadsperioder) {
        if (id != null) {
            throw new IllegalStateException("[Utvikler feil] Kan ikke editere persistert grunnlag");
        }
        var perioder = this.oppgitteSøknader != null ? new HashSet<>(this.oppgitteSøknader.getStartdatoer()) : new HashSet<>(søknadsperioder);
        perioder.addAll(søknadsperioder);
        this.oppgitteSøknader = new UngdomsytelseStartdatoer(perioder);
    }

    void setRelevanteSøknader(UngdomsytelseStartdatoer relevanteSøknader) {
        Objects.requireNonNull(relevanteSøknader);
        this.relevanteSøknader = relevanteSøknader;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UngdomsytelseStartdatoGrunnlag that = (UngdomsytelseStartdatoGrunnlag) o;
        return Objects.equals(oppgitteSøknader, that.oppgitteSøknader)
            && Objects.equals(relevanteSøknader, that.relevanteSøknader);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgitteSøknader, relevanteSøknader);
    }
}
