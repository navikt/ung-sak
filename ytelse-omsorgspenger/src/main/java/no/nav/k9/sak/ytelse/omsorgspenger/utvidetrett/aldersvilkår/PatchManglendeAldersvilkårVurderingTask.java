package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aldersvilkår;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.VilkårTjeneste;


@ApplicationScoped
@ProsessTask(PatchManglendeAldersvilkårVurderingTask.TASKTYPE)
public class PatchManglendeAldersvilkårVurderingTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "patch.manglende.aldersvilkaar.task";

    private static final Logger log = LoggerFactory.getLogger(PatchManglendeAldersvilkårVurderingTask.class);

    private BehandlingLåsRepository behandlingLåsRepository;
    private BehandlingRepository behandlingRepository;
    private VilkårTjeneste vilkårTjeneste;

    PatchManglendeAldersvilkårVurderingTask() {
        //CDI proxy
    }

    @Inject
    public PatchManglendeAldersvilkårVurderingTask(BehandlingLåsRepository behandlingLåsRepository, BehandlingRepository behandlingRepository, VilkårTjeneste vilkårTjeneste) {
        this.behandlingLåsRepository = behandlingLåsRepository;
        this.behandlingRepository = behandlingRepository;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        Long behandlingId = Long.valueOf(prosessTaskData.getBehandlingId());
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        validerBehandling(behandling);
        BehandlingProsessTask.logContext(behandling);

        BehandlingLås lås = behandlingLåsRepository.taLås(behandlingId);
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), lås);
        LocalDateTimeline<VilkårPeriode> tidslinjeOmsorgenFor = vilkårTjeneste.hentVilkårResultat(behandlingId).getVilkårTimeline(VilkårType.OMSORGEN_FOR);
        DatoIntervallEntitet vilkårsperiode = DatoIntervallEntitet.fra(tidslinjeOmsorgenFor.getMinLocalDate(), tidslinjeOmsorgenFor.getMaxLocalDate());

        Avslagsårsak avslagsårsak = null; //vil sette vilkåret til oppfylt
        vilkårTjeneste.lagreVilkårresultat(kontekst, VilkårType.ALDERSVILKÅR_BARN, vilkårsperiode, avslagsårsak);
        log.info("Setter vilkårsresultatet for ALDERSVLKÅR_BARN til innvilget");
    }

    private void validerBehandling(Behandling behandling) {
        if (behandling.getFagsakYtelseType() != FagsakYtelseType.OMSORGSPENGER_AO) {
            throw new IllegalStateException("Funksjonen er begrenset til å brukes på OMSORGSPENGER_AO");
        }
        if (behandling.getStatus() != BehandlingStatus.IVERKSETTER_VEDTAK) {
            throw new IllegalStateException("Behandlingen er ikke i steg IVED. Løs istedet problemet ved å oppe tilbake til start");
        }
        Vilkårene vilkårresultat = vilkårTjeneste.hentVilkårResultat(behandling.getId());
        Optional<Vilkår> optVilkår = vilkårresultat.getVilkår(VilkårType.ALDERSVILKÅR_BARN);
        if (optVilkår.isEmpty()) {
            throw new IllegalStateException("Behandligen har ikke ALDERSVILKÅR_BARN");
        }
        Vilkår vilkår = optVilkår.get();
        if (!vilkår.getPerioder().isEmpty() && vilkår.getPerioder().stream().noneMatch(vp -> vp.getUtfall() == Utfall.IKKE_VURDERT)) {
            throw new IllegalStateException("Behandlingen perioder, men ingen som er IKKE_VURDERT for aldersvilkåret");
        }
    }

}
