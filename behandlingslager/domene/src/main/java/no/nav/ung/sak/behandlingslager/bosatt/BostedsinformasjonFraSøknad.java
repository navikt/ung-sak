package no.nav.ung.sak.behandlingslager.bosatt;

import jakarta.persistence.*;
import no.nav.ung.sak.behandlingslager.BaseEntitet;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Bostedsopplysning oppgitt av bruker i søknaden.
 * Koblet til {@link BostedsGrunnlag} via {@code bosatt_soeknad_grunnlag_id}.
 */
@Entity(name = "BostedsinformasjonFraSøknad")
@Table(name = "BOSATT_INFORMASJON_FRA_SOEKNAD")
public class BostedsinformasjonFraSøknad extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BOSATT_INFORMASJON_FRA_SOEKNAD")
    private Long id;

    @Column(name = "journalpost_id", nullable = false, updatable = false)
    private String journalpostId;

    @Column(name = "fom_dato", nullable = false, updatable = false)
    private LocalDate fomDato;

    @Column(name = "er_bosatt_i_trondheim", nullable = false, updatable = false)
    private boolean erBosattITrondheim;

    public BostedsinformasjonFraSøknad() {
        // Hibernate
    }

    public BostedsinformasjonFraSøknad(String journalpostId, LocalDate fomDato, boolean erBosattITrondheim) {
        this.journalpostId = journalpostId;
        this.fomDato = fomDato;
        this.erBosattITrondheim = erBosattITrondheim;
    }

    public Long getId() {
        return id;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public boolean isErBosattITrondheim() {
        return erBosattITrondheim;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BostedsinformasjonFraSøknad that)) return false;
        return erBosattITrondheim == that.erBosattITrondheim
            && Objects.equals(journalpostId, that.journalpostId)
            && Objects.equals(fomDato, that.fomDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId, fomDato, erBosattITrondheim);
    }

    @Override
    public String toString() {
        return "BostedsinformasjonFraSøknad{journalpostId=" + journalpostId
            + ", fomDato=" + fomDato
            + ", erBosattITrondheim=" + erBosattITrondheim + '}';
    }
}
