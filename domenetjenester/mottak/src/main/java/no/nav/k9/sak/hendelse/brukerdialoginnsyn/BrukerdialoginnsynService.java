package no.nav.k9.sak.hendelse.brukerdialoginnsyn;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;

@ApplicationScoped
public class BrukerdialoginnsynService {

    private ProsessTaskRepository prosessTaskRepository;
    private boolean enableBrukerdialoginnsyn;
    
    
    public BrukerdialoginnsynService() {
        
    }
    
    @Inject
    public BrukerdialoginnsynService(ProsessTaskRepository prosessTaskRepository,
            @KonfigVerdi(value = "ENABLE_BRUKERDIALOGINNSYN", defaultVerdi = "false") boolean enableBrukerdialoginnsyn) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.enableBrukerdialoginnsyn = enableBrukerdialoginnsyn;
    }
    
    
    public void publiserDokumentHendelse(Behandling behandling, MottattDokument mottattDokument) {
        if (!enableBrukerdialoginnsyn || behandling.getFagsakYtelseType() != FagsakYtelseType.PLEIEPENGER_SYKT_BARN) {
            return;
        }
        final ProsessTaskData pd = PubliserSøknadForBrukerdialoginnsynTask.createProsessTaskData(behandling, mottattDokument);
        prosessTaskRepository.lagre(pd);
    }
}
