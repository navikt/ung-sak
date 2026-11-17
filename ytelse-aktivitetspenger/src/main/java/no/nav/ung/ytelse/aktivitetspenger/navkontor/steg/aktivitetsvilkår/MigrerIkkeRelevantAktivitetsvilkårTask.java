package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.aktivitetsvilkår;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.InngangsvilkårVurderingTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Engangsmigrering: VurderAktivitetsvilkårSteg satte en periode automatisk til IKKE_RELEVANT i stedet for å innvilge den.
 * Tasken gir disse periodene samme resultat som automatisk innvilgelse i steget. Idempotent.
 */
@ApplicationScoped
@ProsessTask(value = MigrerIkkeRelevantAktivitetsvilkårTask.TASKTYPE, maxFailedRuns = 1)
public class MigrerIkkeRelevantAktivitetsvilkårTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "avp.migrerIkkeRelevantAktivitetsvilkaar";

    static final String AUTOMATISK_IKKE_RELEVANT_BEGRUNNELSE = "Automatisk vurdering: Ingen vurdering av aktivitetsvilkår for ny søkt periode";

    private static final Logger log = LoggerFactory.getLogger(MigrerIkkeRelevantAktivitetsvilkårTask.class);

    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste;

    MigrerIkkeRelevantAktivitetsvilkårTask() {
        // CDI
    }

    @Inject
    public MigrerIkkeRelevantAktivitetsvilkårTask(EntityManager entityManager,
                                                  BehandlingRepository behandlingRepository,
                                                  VilkårResultatRepository vilkårResultatRepository,
                                                  InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository,
                                                  InngangsvilkårVurderingTjeneste inngangsvilkårVurderingTjeneste) {
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
        this.inngangsvilkårVurderingTjeneste = inngangsvilkårVurderingTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var behandlingIder = finnBehandlingerMedAutomatiskIkkeRelevantAktivitetsvilkår();
        log.info("Fant {} behandlinger med aktivitetsvilkår automatisk satt til IKKE_RELEVANT", behandlingIder.size());
        behandlingIder.forEach(this::migrer);
    }

    private List<Long> finnBehandlingerMedAutomatiskIkkeRelevantAktivitetsvilkår() {
        @SuppressWarnings("unchecked")
        List<Number> resultat = entityManager.createNativeQuery(
                "select distinct rs.behandling_id " +
                    "from rs_vilkars_resultat rs " +
                    "join vr_vilkar v on v.vilkar_resultat_id = rs.vilkarene_id " +
                    "join vr_vilkar_periode vp on vp.vilkar_id = v.id " +
                    "where rs.aktiv = true " +
                    "and v.vilkar_type = :vilkarType " +
                    "and vp.utfall = :utfall " +
                    "and vp.begrunnelse = :begrunnelse")
            .setParameter("vilkarType", VilkårType.AKTIVITETSVILKÅR.getKode())
            .setParameter("utfall", Utfall.IKKE_RELEVANT.getKode())
            .setParameter("begrunnelse", AUTOMATISK_IKKE_RELEVANT_BEGRUNNELSE)
            .getResultList();
        return resultat.stream().map(Number::longValue).toList();
    }

    private void migrer(Long behandlingId) {
        behandlingRepository.taSkriveLås(behandlingId);
        var vilkårene = vilkårResultatRepository.hent(behandlingId);
        var automatiskInnvilget = vilkårene.getVilkår(VilkårType.AKTIVITETSVILKÅR).orElseThrow().getPerioder().stream()
            .filter(p -> p.getUtfall() == Utfall.IKKE_RELEVANT && AUTOMATISK_IKKE_RELEVANT_BEGRUNNELSE.equals(p.getBegrunnelse()))
            .map(p -> new AktivitetsvilkårResultatPeriode(p.getPeriode(), true, null, false, null, null, "ung-sak", LocalDateTime.now(), null))
            .toList();

        var vilkårBuilder = Vilkårene.builderFraEksisterende(vilkårene);
        inngangsvilkårVurderingRepository.lagreAktivitetVurderinger(behandlingId, automatiskInnvilget);
        inngangsvilkårVurderingTjeneste.settAktivitetsvilkårResultat(behandlingId, vilkårBuilder);
        vilkårResultatRepository.lagre(behandlingId, vilkårBuilder.build());
        log.info("Migrerte {} perioder av aktivitetsvilkåret til OPPFYLT for behandling {}", automatiskInnvilget.size(), behandlingId);
    }
}
