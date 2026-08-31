package no.nav.ung.sak.behandlingslager.vilkårsavklaring;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Dependent
public class VilkårsavklaringGrunnlagRepository {

    private static final Logger LOG = LoggerFactory.getLogger(VilkårsavklaringGrunnlagRepository.class);

    private final EntityManager entityManager;

    @Inject
    public VilkårsavklaringGrunnlagRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<VilkårsavklaringGrunnlag> hentGrunnlagHvisEksisterer(Long behandlingId, VilkårType vilkårType) {
        var query = entityManager.createQuery(
            "SELECT g FROM VilkårsavklaringGrunnlag g " +
                "WHERE g.behandlingId = :behandlingId AND g.vilkårType = :vilkårType AND g.aktiv = true",
            VilkårsavklaringGrunnlag.class);
        query.setParameter("behandlingId", behandlingId);
        query.setParameter("vilkårType", vilkårType);
        return HibernateVerktøy.hentUniktResultat(query);
    }

    /**
     * Lagrer saksbehandlers foreslåtte vilkårsavklaringer for en behandling og et vilkår. Oppretter grunnlaget
     * dersom det ikke finnes fra før (i motsetning til bosted, der grunnlaget alltid er opprettet av
     * søknadsmottaket på forhånd).
     *
     * @return avklaringene som nå er foreslått på behandlingen for dette vilkåret
     */
    public Set<VilkårPeriodeAvklaring> lagreForeslåtteAvklaringer(Long behandlingId, VilkårType vilkårType, Set<VilkårPeriodeAvklaringForeslått> nyeAvklaringer) {
        var eksisterendeGrunnlag = hentGrunnlagHvisEksisterer(behandlingId, vilkårType);
        var nyttGrunnlag = eksisterendeGrunnlag
            .map(VilkårsavklaringGrunnlag::nyttGrunnlagMedReferanserFra)
            .orElseGet(() -> new VilkårsavklaringGrunnlag(behandlingId, vilkårType));

        nyttGrunnlag.setForeslåtteAvklaringer(nyeAvklaringer);

        if (eksisterendeGrunnlag.isPresent() && eksisterendeGrunnlag.get().equals(nyttGrunnlag)) {
            LOG.info("lagreForeslåtteAvklaringer ga ingen endring i vilkårsavklaringgrunnlag for behandlingId={}, vilkårType={}", behandlingId, vilkårType);
            return new HashSet<>(eksisterendeGrunnlag.get().getForeslåtteAvklaringer());
        }
        eksisterendeGrunnlag.ifPresent(this::deaktiverEksisterende);

        entityManager.persist(nyttGrunnlag);
        entityManager.flush();

        return new HashSet<>(nyttGrunnlag.getForeslåtteAvklaringer());
    }

    public void ferdigstillForeslåtteAvklaringer(long behandlingId, VilkårType vilkårType) {
        var eksisterendeGrunnlag = hentGrunnlagHvisEksisterer(behandlingId, vilkårType);
        if (eksisterendeGrunnlag.isEmpty()) {
            LOG.info("ferdigstillForeslåtteAvklaringer fant ingen vilkårsavklaringgrunnlag for behandlingId={}, vilkårType={}", behandlingId, vilkårType);
            return;
        }

        var nyttGrunnlag = VilkårsavklaringGrunnlag.nyttGrunnlagMedReferanserFra(eksisterendeGrunnlag.get());
        nyttGrunnlag.ferdigstillForeslåtteAvklaringer();

        if (eksisterendeGrunnlag.get().equals(nyttGrunnlag)) {
            LOG.info("ferdigstillForeslåtteAvklaringer ga ingen endring i vilkårsavklaringgrunnlag for behandlingId={}, vilkårType={}", behandlingId, vilkårType);
            return;
        }
        deaktiverEksisterende(eksisterendeGrunnlag.get());

        entityManager.persist(nyttGrunnlag);
        entityManager.flush();
    }

    /**
     * Kopierer grunnlag fra en eksisterende behandling til en ny behandling — for alle vilkårstyper som har et
     * grunnlag på den gamle behandlingen. Holderen (ferdigstilte avklaringer) refereres uten kopiering. Foreslåtte
     * avklaringer utelates strukturelt, siden de kun gjelder den behandlingen de ble foreslått i.
     */
    public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
        for (VilkårType vilkårType : VilkårType.values()) {
            hentGrunnlagHvisEksisterer(gammelBehandlingId, vilkårType).ifPresent(eksisterende -> {
                var nyttGrunnlag = VilkårsavklaringGrunnlag.nyttGrunnlagForBehandlingMedReferanserFra(nyBehandlingId, eksisterende);
                entityManager.persist(nyttGrunnlag);
                entityManager.flush();
            });
        }
    }

    private void deaktiverEksisterende(VilkårsavklaringGrunnlag gr) {
        gr.deaktiver();
        entityManager.persist(gr);
        entityManager.flush();
    }
}
