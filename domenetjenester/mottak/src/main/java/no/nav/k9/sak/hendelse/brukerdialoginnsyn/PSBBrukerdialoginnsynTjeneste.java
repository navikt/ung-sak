package no.nav.k9.sak.hendelse.brukerdialoginnsyn;

import java.time.ZonedDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.innsyn.InnsynHendelse;
import no.nav.k9.innsyn.Omsorg;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.domene.behandling.steg.omsorgenfor.BrukerdialoginnsynTjeneste;
import no.nav.k9.søknad.JsonUtils;

@FagsakYtelseTypeRef(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
@ApplicationScoped
public class PSBBrukerdialoginnsynTjeneste implements BrukerdialoginnsynTjeneste {

    private ProsessTaskTjeneste prosessTaskRepository;
    private boolean enableBrukerdialoginnsyn;


    public PSBBrukerdialoginnsynTjeneste() {

    }

    @Inject
    public PSBBrukerdialoginnsynTjeneste(ProsessTaskTjeneste prosessTaskRepository,
                                         @KonfigVerdi(value = "ENABLE_BRUKERDIALOGINNSYN", defaultVerdi = "false") boolean enableBrukerdialoginnsyn) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.enableBrukerdialoginnsyn = enableBrukerdialoginnsyn;
    }


    public void publiserDokumentHendelse(Behandling behandling, MottattDokument mottattDokument) {
        if (!enableBrukerdialoginnsyn) {
            return;
        }
        final ProsessTaskData pd = PubliserSøknadForBrukerdialoginnsynTask.createProsessTaskData(behandling, mottattDokument);
        prosessTaskRepository.lagre(pd);
    }

    public void publiserOmsorgenForHendelse(Behandling behandling, boolean harOmsorg) {
        if (!enableBrukerdialoginnsyn) {
            return;
        }

        final InnsynHendelse<Omsorg> hendelse = new InnsynHendelse<>(ZonedDateTime.now(), new Omsorg(
                behandling.getFagsak().getAktørId().getId(),
                behandling.getFagsak().getPleietrengendeAktørId().getId(),
                harOmsorg));

        final String json = JsonUtils.toString(hendelse);
        final ProsessTaskData pd = PubliserJsonForBrukerdialoginnsynTask.createProsessTaskData(behandling, json);
        prosessTaskRepository.lagre(pd);
    }
}
