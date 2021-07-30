package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Dependent
public class OmsorgspengerGrunnlagRepository {

    private EntityManager entityManager;

    @Inject
    public OmsorgspengerGrunnlagRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Optional<OppgittFravær> hentOppittFraværHvisEksisterer(UUID behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(OmsorgspengerGrunnlag::getOppgittFravær);
    }

    // Henter sammenslåtte kravperioder fra IM-er og søknader
    Optional<OppgittFravær> hentOppgittFraværHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(OmsorgspengerGrunnlag::getOppgittFravær);
    }

    // Henter kravperioder fra søknader (rått, uten sammenslåing)
    public Optional<OppgittFravær> hentOppgittFraværFraSøknadHvisEksisterer(Long behandlingId) {
        if (behandlingId == null) {
            return Optional.empty();
        }
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(OmsorgspengerGrunnlag::getOppgittFraværFraSøknad);
    }

    public Set<OppgittFraværPeriode> hentAlleFraværPerioder(Long behandlingId) {
        if (behandlingId == null) {
            return Set.of();
        }
        final var grunnlag = hentGrunnlag(behandlingId).orElse(null);
        if (grunnlag == null || grunnlag.getOppgittFravær() == null) {
            return Set.of();
        }
        return grunnlag.getOppgittFravær().getPerioder();
    }

    public Optional<DatoIntervallEntitet> hentMaksPeriode(Long behandlingId) {
        var perioder = hentAlleFraværPerioder(behandlingId);
        if (perioder.isEmpty()) {
            return Optional.empty();
        }

        var fom = perioder.stream()
            .filter(it -> !Duration.ZERO.equals(it.getFraværPerDag()))
            .map(OppgittFraværPeriode::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .min(LocalDate::compareTo);
        var tom = perioder.stream()
            .filter(it -> !Duration.ZERO.equals(it.getFraværPerDag()))
            .map(OppgittFraværPeriode::getPeriode)
            .map(DatoIntervallEntitet::getTomDato)
            .max(LocalDate::compareTo);

        if (tom.isEmpty() && fom.isEmpty()) {
            fom = perioder.stream()
                .map(OppgittFraværPeriode::getPeriode)
                .map(DatoIntervallEntitet::getFomDato)
                .min(LocalDate::compareTo);
            tom = perioder.stream()
                .map(OppgittFraværPeriode::getPeriode)
                .map(DatoIntervallEntitet::getTomDato)
                .max(LocalDate::compareTo);
        }

        return Optional.of(DatoIntervallEntitet.fraOgMedTilOgMed(fom.orElseThrow(), tom.orElseThrow()));
    }

    Optional<OmsorgspengerGrunnlag> hentGrunnlagBasertPåId(Long grunnlagId) {
        return hentEksisterendeGrunnlagBasertPåGrunnlagId(grunnlagId);
    }

    // Lagre sammenslåtte kravperioder fra IM-er og søknader
    public void lagreOgFlushOppgittFravær(Long behandlingId, OppgittFravær input) {
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);
        var eksisterendeFraværFraSøknad = eksisterendeGrunnlag.map(OmsorgspengerGrunnlag::getOppgittFraværFraSøknad).orElse(null);
        entityManager.persist(input);
        deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
        lagreOgFlushNyttGrunnlag(new OmsorgspengerGrunnlag(behandlingId, input, eksisterendeFraværFraSøknad));
    }

    // Lagre kravperioder (uten sammenslåing) fra søknad
    public void lagreOgFlushOppgittFraværFraSøknad(Long behandlingId, OppgittFravær input) {
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);
        var eksisterendeFravær = eksisterendeGrunnlag.map(OmsorgspengerGrunnlag::getOppgittFravær).orElse(null);
        entityManager.persist(input);
        deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
        lagreOgFlushNyttGrunnlag(new OmsorgspengerGrunnlag(behandlingId, eksisterendeFravær, input));
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        Optional<OppgittFravær> søknadEntitetIm = hentOppgittFraværHvisEksisterer(gammelBehandlingId);
        søknadEntitetIm.ifPresent(entitet -> lagreOgFlushOppgittFravær(nyBehandlingId, entitet));
        Optional<OppgittFravær> oppgittFraværSøknad = hentOppgittFraværFraSøknadHvisEksisterer(gammelBehandlingId);
        oppgittFraværSøknad.ifPresent(entitet -> lagreOgFlushOppgittFravær(nyBehandlingId, entitet));
    }

    private void lagreOgFlushNyttGrunnlag(OmsorgspengerGrunnlag grunnlagEntitet) {
        entityManager.persist(grunnlagEntitet);
        entityManager.flush();
    }

    public Optional<OmsorgspengerGrunnlag> hentGrunnlag(Long behandlingId) {
        final TypedQuery<OmsorgspengerGrunnlag> query = entityManager.createQuery(
            "SELECT s FROM OmsorgspengerGrunnlag s " +
                "WHERE s.behandlingId = :behandlingId AND s.aktiv = true",
            OmsorgspengerGrunnlag.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public Optional<OmsorgspengerGrunnlag> hentGrunnlag(UUID behandlingId) {
        final TypedQuery<OmsorgspengerGrunnlag> query = entityManager.createQuery(
            "SELECT s FROM OmsorgspengerGrunnlag s INNER JOIN Behandling b on b.id=s.behandlingId " +
                "WHERE b.uuid = :behandlingId AND s.aktiv = true",
            OmsorgspengerGrunnlag.class);

        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    private void deaktiverEksisterendeGrunnlag(OmsorgspengerGrunnlag eksisterende) {
        if (eksisterende == null) {
            return;
        }
        eksisterende.setAktiv(false);
        lagreOgFlushNyttGrunnlag(eksisterende);
    }

    private Optional<OmsorgspengerGrunnlag> hentEksisterendeGrunnlagBasertPåGrunnlagId(Long id) {
        var query = entityManager.createQuery(
            "SELECT s " +
                "FROM OmsorgspengerGrunnlag s " +
                "WHERE s.id = :grunnlagId ", OmsorgspengerGrunnlag.class);

        query.setParameter("grunnlagId", id);

        return HibernateVerktøy.hentUniktResultat(query);
    }

}
