package no.nav.ung.sak.behandlingslager.behandling.vilkår;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import no.nav.k9.felles.jpa.HibernateVerktøy;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.diff.DiffEntity;
import no.nav.ung.sak.behandlingslager.diff.TraverseEntityGraphFactory;
import no.nav.ung.sak.behandlingslager.diff.TraverseGraph;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;

@Dependent
public class VilkårResultatRepository {

    private static final Logger log = LoggerFactory.getLogger(VilkårResultatRepository.class);
    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;

    public VilkårResultatRepository() {
        // for CDI proxy
    }

    @Inject
    public VilkårResultatRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.behandlingRepository = new BehandlingRepository(entityManager);
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
        lagre(behandlingId, resultat, null);
    }

    public void lagre(Long behandlingId, Vilkårene resultat, DatoIntervallEntitet fagsakPeriode) {
        Objects.requireNonNull(resultat, "Vilkårsresultat");

        var nyttResultat = resultat;
        if (fagsakPeriode != null) {
            nyttResultat = new VilkårResultatBuilder(resultat)
                .medBoundry(fagsakPeriode, true)
                .build();
        }

        var vilkårsResultat = hentVilkårsResultat(behandlingId);
        Vilkårene gammeltResultat = vilkårsResultat.map(VilkårsResultat::getVilkårene).orElse(null);

        var differ = vilkårsDiffer();

        if (differ.areDifferent(gammeltResultat, nyttResultat)) {
            vilkårsResultat.ifPresent(this::deaktiverVilkårsResultat);

            var nyttVilkårsResultat = new VilkårsResultat(behandlingId, nyttResultat);
            entityManager.persist(nyttResultat);
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

        var behandling = behandlingRepository.hentBehandling(tilBehandlingId);
        var fagsakPeriode = behandling.getFagsak().getPeriode();

        lagre(tilBehandlingId, fraBehandlingVilkår.get(), fagsakPeriode);
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

    /**
     * Optimalisert spørring for å hente vilkårsresultater uten regelsporing.
     */
    public List<VilkårPeriodeResultatDto> hentVilkårResultater(Long behandlingId) {
        String sql = "select vv.vilkar_type, vp.fom, vp.tom, nullif(vp.utfall, '-') as utfall, nullif(vp.overstyrt_utfall, '-') as overstyrt_utfall, vp.avslag_kode" +
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
            var fom = t.get("fom", LocalDate.class);
            var tom = t.get("tom", LocalDate.class);
            var avslagsårsak = Avslagsårsak.fraKode(t.get("avslag_kode", String.class));
            var utfall = Utfall.fraKode(t.get("utfall", String.class));
            var overstyrtUtfall = Utfall.fraKode(t.get("overstyrt_utfall", String.class));
            var angittUtfall = overstyrtUtfall != null ? overstyrtUtfall : utfall;
            dtoer.add(new VilkårPeriodeResultatDto(vt, new Periode(fom, tom), avslagsårsak, angittUtfall));
        }

        return List.copyOf(dtoer);

    }

    public void tilbakestillPerioder(Long behandlingId, VilkårType vilkårType, KantIKantVurderer kantIKantVurderer, NavigableSet<DatoIntervallEntitet> vilkårsPeriode) {
        Optional<Vilkårene> vilkårResultatOpt = this.hentHvisEksisterer(behandlingId);
        if (vilkårResultatOpt.isEmpty()) {
            return;
        }
        Vilkårene vilkårene = vilkårResultatOpt.get();
        Optional<Vilkår> vilkårOpt = vilkårene.getVilkårene().stream()
            .filter(v -> v.getVilkårType().equals(vilkårType))
            .findFirst();
        if (vilkårOpt.isEmpty()) {
            return;
        }
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(vilkårType)
            .medKantIKantVurderer(kantIKantVurderer);
        for (var periode : vilkårsPeriode) {
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode).medUtfall(Utfall.IKKE_VURDERT));
        }

        builder.leggTil(vilkårBuilder);
        var nyttResultat = builder.build();
        this.lagre(behandlingId, nyttResultat);
    }
}
