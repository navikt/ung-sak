package no.nav.ung.sak.behandlingslager.behandling.klage;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.kodeverk.klage.KlageVurdertAv;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Dependent
public class KlageRepository {

    private EntityManager entityManager;

    protected KlageRepository() {
        // for CDI proxy
    }

    @Inject
    public KlageRepository(EntityManager entityManager) {
        Objects.requireNonNull(entityManager, "entityManager"); //$NON-NLS-1$
        this.entityManager = entityManager;
    }

    public Optional<KlageVurderingEntitet> hentGjeldendeVurdering(Behandling behandling) {
        List<KlageVurderingEntitet> klageFormkravListe = hentAlleVurderinger(behandling.getId());

        if (klageFormkravListe.stream().filter(v1 -> v1.getVurdertAvEnhet().erKlageenhet()).count() > 1) {
            throw new IllegalStateException("Fant flere enn 1 klagevurderinger gjort av klageenheten");
        }

        return klageFormkravListe.stream()
            .filter(kf -> kf.getVurdertAvEnhet().erKlageenhet())
            .findFirst()
            .or(() -> klageFormkravListe.stream()
                .filter(kf -> KlageVurdertAv.NAY.equals(kf.getVurdertAvEnhet()))
                .findFirst());
    }

    private List<KlageVurderingEntitet> hentAlleVurderinger(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$

        final TypedQuery<KlageVurderingEntitet> query = entityManager.createQuery(
            "SELECT k FROM KlageVurderingEntitet k" +
                "        WHERE k.behandlingId = :behandlingId", //$NON-NLS-1$
            KlageVurderingEntitet.class);// NOSONAR
        query.setParameter("behandlingId", behandlingId);
        return query.getResultList();
    }

    public KlageUtredningEntitet hentKlageUtredning(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$

        final TypedQuery<KlageUtredningEntitet> query = entityManager.createQuery(
            "SELECT k FROM KlageUtredning k" +
                "   WHERE k.behandlingId = :behandlingId", //$NON-NLS-1$
            KlageUtredningEntitet.class);// NOSONAR

        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentEksaktResultat(query);
    }

    public Optional<KlageUtredningEntitet> finnKlageUtredning(Long behandlingId) {
        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$

        final TypedQuery<KlageUtredningEntitet> query = entityManager.createQuery(
            "SELECT k FROM KlageUtredning k " +
                "   WHERE k.behandlingId = :behandlingId", //$NON-NLS-1$
            KlageUtredningEntitet.class);// NOSONAR

        query.setParameter("behandlingId", behandlingId);
        return HibernateVerktøy.hentUniktResultat(query);
    }

//
//    public Optional<KlageUtredningResultatEntitet> hentUtredningresultat(Long behandlingId) {
//        Objects.requireNonNull(behandlingId, "behandlingId"); // NOSONAR //$NON-NLS-1$
//
//        final TypedQuery<KlageUtredningResultatEntitet> query = entityManager.createQuery(
//            "SELECT k FROM KlageUtredningResultatEntitet k" +
//                "        WHERE k.behandlingId = :behandlingId" +
//                "        AND k.vurdertAvEnhet = :enhet", //$NON-NLS-1$
//            KlageUtredningResultatEntitet.class);// NOSONAR
//        query.setParameter("behandlingId", behandlingId);
//        query.setParameter("enhet", enhet);
//        return HibernateVerktøy.hentUniktResultat(query);
//    }

//
//    public void slettKlageResultat(Long klageBehandlingId, KlageVurdertAv klageVurdertAv) {
//        Optional<KlageUtredningResultatEntitet> KlageVurderingEntitetOptional = hentUtredningresultat(klageBehandlingId, klageVurdertAv);
//        KlageVurderingEntitetOptional.ifPresent(vurdering -> {
//            if (vurdering.harKlageresultat()) {
//                vurdering.fjernResultat();
//                lagre(vurdering);
//            }
//        });
//    }
//
//    public void slettVurdering(Long klageBehandlingId, KlageVurdertAv klageVurdertAv) {
//        Optional<KlageUtredningResultatEntitet> klageVurderingOpt = hentUtredningresultat(klageBehandlingId, klageVurdertAv);
//        klageVurderingOpt.ifPresent(vurdering -> {
//            entityManager.remove(vurdering);
//            entityManager.flush();
//        });
//    }

//    public void slettFormkrav(Long klageBehandlingId, KlageVurdertAv klageVurdertAv) {
//        Optional<KlageUtredningResultatEntitet> klageVurderingOpt = hentUtredningresultat(klageBehandlingId, klageVurdertAv);
//        klageVurderingOpt.ifPresent(vurdering -> {
//            vurdering.slettFormkrav();
//            lagre(vurdering);
//        });
//    }
//
//    public void lagre(KlageUtredningResultatEntitet klageVurdering) {
//        entityManager.persist(klageVurdering);
//        entityManager.flush();
//    }

    public void lagre(KlageUtredningEntitet påklagdBehandling) {
        entityManager.persist(påklagdBehandling);
        entityManager.flush();
    }
}
