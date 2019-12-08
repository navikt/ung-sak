package no.nav.foreldrepenger.behandlingslager.uttak;

import static no.nav.vedtak.felles.jpa.HibernateVerktøy.hentUniktResultat;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;

@ApplicationScoped
public class UttakRepository  {

    private EntityManager entityManager;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    public UttakRepository(@VLPersistenceUnit EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
        this.behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
    }

    protected UttakRepository() {
        // CDI proxy
    }

    public void lagreOpprinneligUttakResultatPerioder(Long behandlingId, UttakResultatPerioderEntitet opprinneligPerioder) {
        lagreUttaksresultat(behandlingId, builder -> builder.nullstill().medOpprinneligPerioder(opprinneligPerioder));
    }

    private void lagreUttaksresultat(Long behandlingId, Function<UttakResultatEntitet.Builder, UttakResultatEntitet.Builder> resultatTransformator) {
        Optional<UttakResultatEntitet> eksistrendeResultat = hentUttakResultatHvisEksisterer(behandlingId);

        UttakResultatEntitet.Builder builder = new UttakResultatEntitet.Builder(hentBeregningsresultat(behandlingId));
        if (eksistrendeResultat.isPresent()) {
            UttakResultatEntitet eksisterende = eksistrendeResultat.get();
            if (eksisterende.getOpprinneligPerioder() != null) {
                builder.medOpprinneligPerioder(eksisterende.getOpprinneligPerioder());
            }
            if (eksisterende.getOverstyrtPerioder() != null) {
                builder.medOverstyrtPerioder(eksisterende.getOverstyrtPerioder());
            }
            deaktiverResultat(eksisterende);
        }
        builder = resultatTransformator.apply(builder);

        UttakResultatEntitet nyttResultat = builder.build();

        persistResultat(nyttResultat);
        entityManager.flush();
    }

    private Behandlingsresultat hentBeregningsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Må ha beregningsresultat ved lagring av uttak"));
    }

    private void persistResultat(UttakResultatEntitet resultat) {
        UttakResultatPerioderEntitet overstyrtPerioder = resultat.getOverstyrtPerioder();
        if (overstyrtPerioder != null) {
            persistPerioder(overstyrtPerioder);
        }
        UttakResultatPerioderEntitet opprinneligPerioder = resultat.getOpprinneligPerioder();
        if (opprinneligPerioder != null) {
            persistPerioder(opprinneligPerioder);
        }
        entityManager.persist(resultat);
    }

    private void persistPerioder(UttakResultatPerioderEntitet perioder) {
        entityManager.persist(perioder);
        for (UttakResultatPeriodeEntitet periode : perioder.getPerioder()) {
            persisterPeriode(periode);
        }
    }

    private void persisterPeriode(UttakResultatPeriodeEntitet periode) {
        entityManager.persist(periode);
    }

    private void deaktiverResultat(UttakResultatEntitet resultat) {
        resultat.deaktiver();
        entityManager.persist(resultat);
        entityManager.flush();
    }

    public Optional<UttakResultatEntitet> hentUttakResultatHvisEksisterer(Long behandlingId) {
        TypedQuery<UttakResultatEntitet> query = entityManager.createQuery(
            "select uttakResultat from UttakResultatEntitet uttakResultat " +
                "join uttakResultat.behandlingsresultat resultat" +
                " where resultat.behandling.id=:behandlingId and uttakResultat.aktiv=TRUE", UttakResultatEntitet.class); //$NON-NLS-1$
        query.setParameter("behandlingId", behandlingId); // NOSONAR //$NON-NLS-1$
        return hentUniktResultat(query);
    }

    public UttakResultatEntitet hentUttakResultat(Long behandlingId) {
        Optional<UttakResultatEntitet> resultat = hentUttakResultatHvisEksisterer(behandlingId);
        return resultat.orElseThrow(() -> new NoResultException("Fant ikke uttak resultat på behandlingen " + behandlingId + ", selv om det var forventet."));
    }


}
