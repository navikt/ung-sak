package no.nav.k9.sak.ytelse.omsorgspenger.repo;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    // Henter sammenslåtte kravperioder fra IM-er, søknader og fraværskorrigeringer av IM-er
    public Optional<OppgittFravær> hentSamletOppgittFraværHvisEksisterer(Long behandlingId) {
        Objects.requireNonNull(behandlingId);
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(OmsorgspengerGrunnlag::getSamletOppgittFravær);
    }

    // Henter kravperioder fra søknader (rått, uten sammenslåing)
    public Optional<OppgittFravær> hentOppgittFraværFraSøknadHvisEksisterer(Long behandlingId) {
        Objects.requireNonNull(behandlingId);
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(OmsorgspengerGrunnlag::getOppgittFraværFraSøknad);
    }

    // Henter kravperioder fra fraværskorrigeringer av IM-er (rått, uten sammenslåing)
    public Optional<OppgittFravær> hentOppgittFraværFraFraværskorrigeringerHvisEksisterer(Long behandlingId) {
        Objects.requireNonNull(behandlingId);
        final var grunnlag = hentGrunnlag(behandlingId);

        return grunnlag.map(OmsorgspengerGrunnlag::getOppgittFraværFraKorrigeringIm);
    }

    // Henter alle sammenslåtte kravperioder fra IM-er, søknader og fraværskorrigeringer av IM-er
    public Set<OppgittFraværPeriode> hentAlleFraværPerioder(Long behandlingId) {
        Objects.requireNonNull(behandlingId);
        final var grunnlag = hentGrunnlag(behandlingId).orElse(null);
        if (grunnlag == null || grunnlag.getSamletOppgittFravær() == null) {
            return Set.of();
        }
        return grunnlag.getSamletOppgittFravær().getPerioder();
    }

    // Henter alle kravperioder fra søknader (rått, uten sammenslåing)
    private Set<OppgittFraværPeriode> hentAlleFraværPerioderFraSøknad(Long behandlingId) {
        Objects.requireNonNull(behandlingId);
        final var grunnlag = hentGrunnlag(behandlingId).orElse(null);
        if (grunnlag == null || grunnlag.getOppgittFraværFraSøknad() == null) {
            return Set.of();
        }
        return grunnlag.getOppgittFraværFraSøknad().getPerioder();
    }

    // Henter alle kravperioder fra fraværskorrigeringer av IM-er (rått, uten sammenslåing)
    private Set<OppgittFraværPeriode> hentAlleFraværskorrigeringerIm(Long behandlingId) {
        Objects.requireNonNull(behandlingId);
        final var grunnlag = hentGrunnlag(behandlingId).orElse(null);
        if (grunnlag == null || grunnlag.getOppgittFraværFraKorrigeringIm() == null) {
            return Set.of();
        }
        return grunnlag.getOppgittFraværFraKorrigeringIm().getPerioder();
    }

    public Optional<DatoIntervallEntitet> hentMaksPeriode(Long behandlingId) {
        var perioderSammenslåtte = hentAlleFraværPerioder(behandlingId);
        var perioderSøknad = hentAlleFraværPerioderFraSøknad(behandlingId);
        var perioderFraværskorrigering = hentAlleFraværskorrigeringerIm(behandlingId);
        if (perioderSammenslåtte.isEmpty() && perioderSøknad.isEmpty() && perioderFraværskorrigering.isEmpty()) {
            return Optional.empty();
        }

        var perioder = Stream.of(perioderSammenslåtte, perioderSøknad, perioderFraværskorrigering).flatMap(Collection::stream).collect(Collectors.toSet());
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

    // Lagre sammenslåtte kravperioder fra IM-er, søknader og fraværskorrigeringer av IM-er
    public void lagreOgFlushSamletOppgittFravær(Long behandlingId, OppgittFravær input) {
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);
        var eksisterendeFraværFraSøknad = eksisterendeGrunnlag.map(OmsorgspengerGrunnlag::getOppgittFraværFraSøknad).orElse(null);
        var eksisterendeFraværskorrigeringerIm = eksisterendeGrunnlag.map(OmsorgspengerGrunnlag::getOppgittFraværFraKorrigeringIm).orElse(null);

        // Input overskriver alle tidligere sammenslåtte perioder
        entityManager.persist(input);

        deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
        lagreOgFlushNyttGrunnlag(new OmsorgspengerGrunnlag(behandlingId, input, eksisterendeFraværFraSøknad, eksisterendeFraværskorrigeringerIm));
    }

    // Lagre kravperioder (uten sammenslåing) fra søknad
    public void lagreOgFlushOppgittFraværFraSøknad(Long behandlingId, Set<OppgittFraværPeriode> nyttFraværFraSøknad) {
        if (nyttFraværFraSøknad.isEmpty()) {
            return;
        }
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);
        var eksisterendeSamletFravær = eksisterendeGrunnlag.map(OmsorgspengerGrunnlag::getSamletOppgittFravær).orElse(null);
        var eksisterendeFraværFraSøknad = eksisterendeGrunnlag.map(OmsorgspengerGrunnlag::getOppgittFraværFraSøknad).orElse(null);
        var eksisterendeFraværskorrigeringerIm = eksisterendeGrunnlag.map(OmsorgspengerGrunnlag::getOppgittFraværFraKorrigeringIm).orElse(null);

        Set<OppgittFraværPeriode> søktFravær = new LinkedHashSet<>();
        if (eksisterendeFraværFraSøknad != null) {
            søktFravær.addAll(eksisterendeFraværFraSøknad.getPerioder());
        }
        søktFravær.addAll(nyttFraværFraSøknad);
        var oppdatertFravær = new OppgittFravær(søktFravær);
        entityManager.persist(oppdatertFravær);

        deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
        lagreOgFlushNyttGrunnlag(new OmsorgspengerGrunnlag(behandlingId, eksisterendeSamletFravær, oppdatertFravær, eksisterendeFraværskorrigeringerIm));
    }

    // Lagre kravperioder (uten sammenslåing) fra søknad
    public void lagreOgFlushFraværskorrigeringerIm(Long behandlingId, Set<OppgittFraværPeriode> nyeFraværskorrigeringerIm) {
        if (nyeFraværskorrigeringerIm.isEmpty()) {
            return;
        }
        var eksisterendeGrunnlag = hentGrunnlag(behandlingId);
        var eksisterendeSamletFravær = eksisterendeGrunnlag.map(OmsorgspengerGrunnlag::getSamletOppgittFravær).orElse(null);
        var eksisterendeFraværFraSøknad = eksisterendeGrunnlag.map(OmsorgspengerGrunnlag::getOppgittFraværFraSøknad).orElse(null);
        var eksisterendeFraværskorrigeringerIm = eksisterendeGrunnlag.map(OmsorgspengerGrunnlag::getOppgittFraværFraKorrigeringIm).orElse(null);

        Set<OppgittFraværPeriode> fraværskorrigeringerIm = new LinkedHashSet<>();
        if (eksisterendeFraværskorrigeringerIm != null) {
            fraværskorrigeringerIm.addAll(eksisterendeFraværskorrigeringerIm.getPerioder());
        }
        fraværskorrigeringerIm.addAll(nyeFraværskorrigeringerIm);
        var oppdatertFraværskorrigeringer = new OppgittFravær(fraværskorrigeringerIm);
        entityManager.persist(oppdatertFraværskorrigeringer);

        deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
        lagreOgFlushNyttGrunnlag(new OmsorgspengerGrunnlag(behandlingId, eksisterendeSamletFravær, eksisterendeFraværFraSøknad, oppdatertFraværskorrigeringer));
    }

    /**
     * Kopierer grunnlag fra en tidligere behandling. Endrer ikke aggregater, en skaper nye referanser til disse.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        var eksisterendeGrunnlag = hentGrunnlag(gammelBehandlingId);
        var samletOppgittFravær = eksisterendeGrunnlag.map(OmsorgspengerGrunnlag::getSamletOppgittFravær);
        var oppgittFraværFraSøknad = eksisterendeGrunnlag.map(OmsorgspengerGrunnlag::getOppgittFraværFraSøknad);
        var oppgittFraværskorrigeringerIm = eksisterendeGrunnlag.map(OmsorgspengerGrunnlag::getOppgittFraværFraKorrigeringIm);

        samletOppgittFravær.ifPresent(entityManager::persist);
        oppgittFraværFraSøknad.ifPresent(entityManager::persist);
        oppgittFraværskorrigeringerIm.ifPresent(entityManager::persist);

        deaktiverEksisterendeGrunnlag(eksisterendeGrunnlag.orElse(null));
        lagreOgFlushNyttGrunnlag(new OmsorgspengerGrunnlag(nyBehandlingId,
            samletOppgittFravær.orElse(null),
            oppgittFraværFraSøknad.orElse(null),
            oppgittFraværskorrigeringerIm.orElse(null)));
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
