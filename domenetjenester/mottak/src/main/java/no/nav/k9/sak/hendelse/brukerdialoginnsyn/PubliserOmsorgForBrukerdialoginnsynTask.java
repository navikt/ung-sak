package no.nav.k9.sak.hendelse.brukerdialoginnsyn;

import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;

@ApplicationScoped
@ProsessTask(PubliserOmsorgForBrukerdialoginnsynTask.TASKTYPE)
public class PubliserOmsorgForBrukerdialoginnsynTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "brukerdialoginnsyn.publiserOmsorg";
    private static final Logger logger = LoggerFactory.getLogger(PubliserOmsorgForBrukerdialoginnsynTask.class);
    
    private BrukerdialoginnsynService brukerdialoginnsynService;
    private BehandlingRepository behandlingRepository;
    private BehandlingModellRepository behandlingModellRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    

    
    public PubliserOmsorgForBrukerdialoginnsynTask() {}

    @Inject
    public PubliserOmsorgForBrukerdialoginnsynTask(BrukerdialoginnsynService brukerdialoginnsynService,
            BehandlingRepository behandlingRepository,
            BehandlingModellRepository behandlingModellRepository,
            VilkårResultatRepository vilkårResultatRepository) {
        this.brukerdialoginnsynService = brukerdialoginnsynService;
        this.behandlingRepository = behandlingRepository;
        this.behandlingModellRepository = behandlingModellRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    
    @Override
    public void doTask(ProsessTaskData pd) {
        final Long fagsakId = Objects.requireNonNull(pd.getFagsakId());
        Optional<Behandling> behandlingOpt = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId);
        if (behandlingOpt.isEmpty()) {
            return;
        }
        
        final Behandling b = behandlingOpt.get();
        if (harKommetForbiOmsorgFor(b)) {
            publiserOmsorgenForHendelseFor(b);
        } else if (b.getOriginalBehandlingId().isPresent()) {
            final Behandling forrigeBehandling = behandlingRepository.hentBehandling(b.getOriginalBehandlingId().get());
            publiserOmsorgenForHendelseFor(forrigeBehandling);
        }
    }

    
    private void publiserOmsorgenForHendelseFor(final Behandling b) {
        final var vilkårene = vilkårResultatRepository.hent(b.getId());
        final boolean harOmsorgenFor = harOmsorgenForISistePeriode(vilkårene);
        brukerdialoginnsynService.publiserOmsorgenForHendelse(b, harOmsorgenFor);
    }
    
    private static boolean harOmsorgenForISistePeriode(Vilkårene vilkårene) {
        final Vilkår vilkår = vilkårene.getVilkår(VilkårType.OMSORGEN_FOR).orElseThrow();
        if (vilkår.getPerioder().isEmpty()) {
            return false;
        }
        final VilkårPeriode vilkårPeriode = vilkår.getPerioder().get(vilkår.getPerioder().size() - 1);
        return (vilkårPeriode.getUtfall() == Utfall.OPPFYLT);
    }
    
    private boolean harKommetForbiOmsorgFor(Behandling behandling) {
        final BehandlingStegType steg = behandling.getAktivtBehandlingSteg();
        final BehandlingModell modell = behandlingModellRepository.getModell(behandling.getType(), behandling.getFagsakYtelseType());
        return modell.erStegAFørStegB(BehandlingStegType.VURDER_OMSORG_FOR, steg);
    }
}
