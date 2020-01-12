package no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@ApplicationScoped
public class TilbakekrevingRepository {

    private EntityManager entityManager;

    TilbakekrevingRepository() {
        // For CDI proxy
    }

    @Inject
    public TilbakekrevingRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<TilbakekrevingValg> hent(Long behandlingId) {
        return hentEntitet(behandlingId).stream().map(this::map).findFirst();
    }

    private Optional<TilbakekrevingValgEntitet> hentEntitet(long behandlingId) {
        List<TilbakekrevingValgEntitet> resultList = entityManager
            .createQuery("from TilbakekrevingValgEntitet where behandlingId=:behandlingId and aktiv=TRUE", TilbakekrevingValgEntitet.class)
            .setParameter("behandlingId", behandlingId)
            .getResultList();

        if (resultList.size() > 1) {
            throw new IllegalStateException(
                "Skal bare kunne finne en aktiv " + TilbakekrevingValg.class.getSimpleName() + " , men fikk flere for behandling " + behandlingId);
        }
        return resultList.isEmpty()
            ? Optional.empty()
            : Optional.of(resultList.get(0));
    }

    private TilbakekrevingValg map(TilbakekrevingValgEntitet entitet) {
        return new TilbakekrevingValg(entitet.erVilkarOppfylt(), entitet.erGrunnTilReduksjon(), entitet.getTilbakekrevningsVidereBehandling(),
            entitet.getVarseltekst());
    }

    public void lagre(Behandling behandling, TilbakekrevingValg valg) {
        deaktiverEksisterendeTilbakekrevingValg(behandling);

        TilbakekrevingValgEntitet nyEntitet = TilbakekrevingValgEntitet.builder()
            .medBehandling(behandling)
            .medVilkarOppfylt(valg.getErTilbakekrevingVilkårOppfylt())
            .medGrunnTilReduksjon(valg.getGrunnerTilReduksjon())
            .medTilbakekrevningsVidereBehandling(valg.getVidereBehandling())
            .medVarseltekst(valg.getVarseltekst())
            .build();

        entityManager.persist(nyEntitet);
        entityManager.flush();
    }

    public void deaktiverEksisterendeTilbakekrevingValg(Behandling behandling) {
        Optional<TilbakekrevingValgEntitet> eksisterende = hentEntitet(behandling.getId());
        if (eksisterende.isPresent()) {
            TilbakekrevingValgEntitet eksisterendeEntitet = eksisterende.get();
            eksisterendeEntitet.deaktiver();
            entityManager.persist(eksisterendeEntitet);
        }
    }

    public void lagre(Behandling behandling, boolean avslåttInntrekk) {
        Objects.requireNonNull(behandling, "behandling");
        deaktiverEksisterendeTilbakekrevingInntrekk(behandling);

        TilbakekrevingInntrekkEntitet inntrekkEntitet = new TilbakekrevingInntrekkEntitet.Builder()
            .medBehandling(behandling)
            .medAvslåttInntrekk(avslåttInntrekk)
            .build();

        entityManager.persist(inntrekkEntitet);
        entityManager.flush();
    }

    public Optional<TilbakekrevingInntrekkEntitet> hentTilbakekrevingInntrekk(Long behandlingId) {
        TypedQuery<TilbakekrevingInntrekkEntitet> query = entityManager
            .createQuery("from TilbakekrevingInntrekkEntitet ti where ti.behandlingId =:behandlingId and aktiv = TRUE", TilbakekrevingInntrekkEntitet.class)
            .setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public void deaktiverEksisterendeTilbakekrevingInntrekk(Behandling behandling) {
        Optional<TilbakekrevingInntrekkEntitet> tilbakekrevingInntrekkEntitet = hentTilbakekrevingInntrekk(behandling.getId());
        if (tilbakekrevingInntrekkEntitet.isPresent()) {
            TilbakekrevingInntrekkEntitet eksisterendeInntrekk = tilbakekrevingInntrekkEntitet.get();
            eksisterendeInntrekk.deaktiver();
            entityManager.persist(eksisterendeInntrekk);
        }
    }
}
