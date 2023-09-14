package no.nav.k9.sak.behandlingslager.notat;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.typer.AktørId;

@Dependent
public class NotatRepository {

    private final EntityManager entityManager;

    @Inject
    public NotatRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public UUID lagre(NotatEntitet notat) {
        entityManager.persist(notat);
        entityManager.flush();
        return notat.getUuid();
    }

    public NotatEntitet hent(UUID notatId) {
        Optional<NotatEntitet> notatPåSak = hentForSak(notatId).map(it -> it);
        Optional<NotatEntitet> notatPåAktør = hentForAktør(notatId).map(it -> it);

        if (notatPåSak.isPresent() && notatPåAktør.isPresent()) {
            throw new IllegalStateException(String.format("Utviklerfeil: Det finnes notat for både sak og aktør med samme uuid=%s", notatId));
        }

        return notatPåSak.orElseGet(() -> notatPåAktør.orElseThrow(() ->
            new NoSuchElementException(String.format("Fant ingen notat med uuid=%s", notatId))));

    }

    private Optional<NotatAktørEntitet> hentForAktør(UUID notatId) {
        TypedQuery<NotatAktørEntitet> queryNotatAktør = entityManager.createQuery(
            "select n from NotatAktørEntitet n where n.uuid = :uuid and aktiv = true", NotatAktørEntitet.class);
        queryNotatAktør.setParameter("uuid", notatId);
        return HibernateVerktøy.hentUniktResultat(queryNotatAktør);
    }

    private Optional<NotatSakEntitet> hentForSak(UUID notatId) {
        TypedQuery<NotatSakEntitet> queryNotatSak = entityManager.createQuery(
            "select n from NotatSakEntitet n where n.uuid = :uuid and aktiv = true", NotatSakEntitet.class);
        queryNotatSak.setParameter("uuid", notatId);
        return HibernateVerktøy.hentUniktResultat(queryNotatSak);
    }

    public List<Notat> hentForSakOgAktør(long fagsakId, FagsakYtelseType ytelseType, AktørId pleietrengendeAktørId) {
        TypedQuery<NotatSakEntitet> sakNotat = entityManager.createQuery(
            "from NotatSakEntitet n where n.fagsakId = :fagsakId and aktiv = true", NotatSakEntitet.class);
        sakNotat.setParameter("fagsakId", fagsakId);

        TypedQuery<NotatAktørEntitet> aktoerNotat = entityManager.createQuery(
            "from NotatAktørEntitet n where n.aktørId = :aktørId and ytelseType = :ytelseType and aktiv = true", NotatAktørEntitet.class);
        aktoerNotat.setParameter("aktørId", pleietrengendeAktørId);
        aktoerNotat.setParameter("ytelseType", ytelseType);

        List<Notat> resultat = new ArrayList<>(sakNotat.getResultList());
        resultat.addAll(aktoerNotat.getResultList());

        return resultat;
    }


    //TODO test at endring fører til flere versjoner

    //TODO test optimistisk låsing


}
