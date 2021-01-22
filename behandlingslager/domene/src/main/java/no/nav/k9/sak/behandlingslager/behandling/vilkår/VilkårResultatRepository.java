package no.nav.k9.sak.behandlingslager.behandling.vilkår;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.diff.DiffEntity;
import no.nav.k9.sak.behandlingslager.diff.TraverseEntityGraphFactory;
import no.nav.k9.sak.behandlingslager.diff.TraverseGraph;
import no.nav.k9.sak.typer.Periode;
import no.nav.vedtak.felles.jpa.HibernateVerktøy;

@Dependent
public class VilkårResultatRepository {

    private static final Logger log = LoggerFactory.getLogger(VilkårResultatRepository.class);
    private EntityManager entityManager;

    public VilkårResultatRepository() {
        // for CDI proxy
    }

    @Inject
    public VilkårResultatRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Vilkårene> hentHvisEksisterer(Long behandlingId) {
        final Optional<VilkårsResultat> vilkårsResultat = hentVilkårsResultat(behandlingId);
        return vilkårsResultat.map(VilkårsResultat::getVilkårene);
    }

    private Optional<VilkårsResultat> hentVilkårsResultat(Long behandlingId) {
        var query = entityManager.createQuery("SELECT vr " +
            "FROM ResultatVilkårResultat vr " +
            "WHERE vr.behandlingId = :behandlingId " +
            "AND vr.aktiv = true", VilkårsResultat.class);
        query.setParameter("behandlingId", behandlingId);

        return HibernateVerktøy.hentUniktResultat(query);
    }

    public Vilkårene hent(Long behandlingId) {
        return hentHvisEksisterer(behandlingId).orElseThrow();
    }

    public void lagre(Long behandlingId, Vilkårene resultat) {
        Objects.requireNonNull(resultat, "Vilkårsresultat");

        var vilkårsResultat = hentVilkårsResultat(behandlingId);
        var differ = vilkårsDiffer();

        if (differ.areDifferent(vilkårsResultat.map(VilkårsResultat::getVilkårene).orElse(null), resultat)) {
            if (vilkårsResultat.isPresent()) {
                deaktiverVilkårsResultat(vilkårsResultat.get());
            }

            var nyttVilkårsResultat = new VilkårsResultat(behandlingId, resultat);
            entityManager.persist(resultat);
            entityManager.persist(nyttVilkårsResultat);
            entityManager.flush();
        } else {
            // Forkaster resultat da ingen diff på vilkårene
            log.info("[behandlingId={}] Forkaster lagring nytt resultat da dette er identisk med eksisterende resultat.", behandlingId);
        }
    }

    public void deaktiverVilkårsResultat(VilkårsResultat vilkårsResultat) {
        vilkårsResultat.setAktiv(false);
        entityManager.persist(vilkårsResultat);
        entityManager.flush();
    }

    public void kopier(Long fraBehandlingId, Long tilBehandlingId) {
        kopieringPrecondition(fraBehandlingId, tilBehandlingId);

        var fraBehandlingVilkår = hentHvisEksisterer(fraBehandlingId);

        if (fraBehandlingVilkår.isEmpty()) {
            return;
        }

        lagre(tilBehandlingId, fraBehandlingVilkår.get());
    }

    private void kopieringPrecondition(Long fraBehandlingId, Long tilBehandlingId) {
        Objects.requireNonNull(fraBehandlingId);
        Objects.requireNonNull(tilBehandlingId);

        var tilBehandlingVilkår = hentHvisEksisterer(tilBehandlingId);
        if (tilBehandlingVilkår.isPresent()) {
            throw new IllegalStateException("Kan ikke kopiere vilkår til en behandling hvor det allerede eksisterer et vilkårsresultat");
        }
    }

    private DiffEntity vilkårsDiffer() {
        TraverseGraph traverser = TraverseEntityGraphFactory.build();
        return new DiffEntity(traverser);
    }

    public void deaktiverVilkårsResultat(Long behandlingId) {
        hentVilkårsResultat(behandlingId).ifPresent(v -> deaktiverVilkårsResultat(v));
    }

    /** Optimalisert spørring for å hente vilkårsresultater uten regelsporing. */
    public List<VilkårPeriodeResultatDto> hentVilkårResultater(Long behandlingId) {
        String sql = "select vv.vilkar_type, vp.fom, vp.tom, nullif('-', vp.utfall) as utfall, nullif('-', vp.overstyrt_utfall) as overstyrt_utfall, vp.avslag_kode" +
            " from rs_vilkars_resultat rv " +
            " inner join vr_vilkar vv on vv.vilkar_resultat_id=rv.vilkarene_id " +
            " inner join vr_vilkar_periode vp on vp.vilkar_id=vv.id " +
            " where rv.aktiv=true and rv.behandling_id = :behandlingId " +
            " order by 1, 2";
        var query = entityManager.createNativeQuery(sql, Tuple.class)
            .setParameter("behandlingId", behandlingId);
        
        @SuppressWarnings("unchecked")
        List<Tuple> resultList = query.getResultList();
        
        List<VilkårPeriodeResultatDto> dtoer = new ArrayList<>();
        for (var t : resultList) {
            var vt = VilkårType.fraKode(t.get("vilkar_type", String.class));
            var fom = t.get("fom", Date.class).toLocalDate();
            var tom = t.get("tom", Date.class).toLocalDate();
            var avslagsårsak = Avslagsårsak.fraKode(t.get("avslag_kode", String.class));
            var utfall = Utfall.fraKode(t.get("utfall", String.class));
            var overstyrtUtfall = Utfall.fraKode(t.get("overstyrt_utfall", String.class));
            var angittUtfall = overstyrtUtfall != null ? overstyrtUtfall : utfall;
            dtoer.add(new VilkårPeriodeResultatDto(vt, new Periode(fom, tom), avslagsårsak, angittUtfall));
        }
        
        return List.copyOf(dtoer);

    }
}
