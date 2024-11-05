package no.nav.k9.sak.domene.vedtak.intern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;

@ApplicationScoped
@ProsessTask(FjernInaktiveVilkårsresultatTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class FjernInaktiveVilkårsresultatTask extends BehandlingProsessTask {


    private static final Logger LOG = LoggerFactory.getLogger(FjernInaktiveVilkårsresultatTask.class);

    public static final String TASKTYPE = "iverksetteVedtak.fjernInaktivtVilkårsresultat";
    private EntityManager entityManager;


    FjernInaktiveVilkårsresultatTask() {
        // for CDI proxy
    }

    @Inject
    public FjernInaktiveVilkårsresultatTask(BehandlingRepositoryProvider repositoryProvider,
                                            EntityManager entityManager) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.entityManager = entityManager;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {

        String rsVilkårResultatSelect = "select rvr.id from rs_vilkars_resultat rvr where rvr.aktiv = false and rvr.behandling_id = :behandlingId and not exists(select 1 from rs_vilkars_resultat rs2 where rs2.vilkarene_id = rvr.vilkarene_id and rs2.aktiv = true";
        var vilkårsperiodeQuery = entityManager.createNativeQuery("""
            delete from vr_vilkar_periode periode where periode.vilkar_id in
                                                (select vilkar.id from vr_vilkar vilkar where vilkar.vilkar_resultat_id in
                                                                                (select vr_resultat.id from VR_VILKAR_RESULTAT vr_resultat where vr_resultat.id in
                                                                                                                         (select rs.vilkarene_id from rs_vilkars_resultat rs where rs.id in
                                                                                                                                                                   (""" + rsVilkårResultatSelect + "))))").setParameter("behandlingId", prosessTaskData.getBehandlingId());

        var vilkårQuery = entityManager.createNativeQuery("""
            delete from vr_vilkar vilkar where vilkar.vilkar_resultat_id in (select vr_resultat.id from VR_VILKAR_RESULTAT vr_resultat where vr_resultat.id in
                                                                                                                             (select rs.vilkarene_id from rs_vilkars_resultat rs where rs.id in
                                                                                                                                                                       (""" + rsVilkårResultatSelect + ")))").setParameter("behandlingId", prosessTaskData.getBehandlingId());

        var vilkåreneQuery = entityManager.createNativeQuery("""
                     delete from VR_VILKAR_RESULTAT vr_resultat where vr_resultat.id in (select rs.vilkarene_id from rs_vilkars_resultat rs where rs.id in
            (""" + rsVilkårResultatSelect + "))").setParameter("behandlingId", prosessTaskData.getBehandlingId());

        var vilkårsresultatQuery = entityManager.createNativeQuery("""
                     delete from rs_vilkars_resultat rs where rs.id in
            (""" + rsVilkårResultatSelect + ")").setParameter("behandlingId", prosessTaskData.getBehandlingId());



        var vilkårsperioderFjernet = vilkårsperiodeQuery.executeUpdate();
        LOG.info("Fjernet {} inaktive vilkårsperioder ", vilkårsperioderFjernet);
        var vilkårFjernet = vilkårQuery.executeUpdate();
        LOG.info("Fjernet {} inaktive vilkår ", vilkårFjernet);
        var vilkåreneFjernet = vilkåreneQuery.executeUpdate();
        LOG.info("Fjernet {} inaktive vilkårene ", vilkåreneFjernet);
        var vilkårsresultatFjernet = vilkårsresultatQuery.executeUpdate();
        LOG.info("Fjernet {} inaktive vilkårsresultat ", vilkårsresultatFjernet);

        if (vilkåreneFjernet != vilkårsresultatFjernet) {
            throw new IllegalStateException("Foventet å fjerne like mange av VR_VILKAR_RESULTAT og RS_VILKAR_RESULTAT");
        }


    }


}
