package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import java.time.LocalDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.iverksett.OpprettProsessTaskIverksett;
import no.nav.k9.sak.domene.vedtak.IdentifiserOverlappendeInfotrygdYtelseTjeneste;
import no.nav.k9.sak.domene.vedtak.impl.VurderBehandlingerUnderIverksettelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@BehandlingStegRef(kode = "IVEDSTEG")
@BehandlingTypeRef("BT-004") // Revurdering
@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class IverksetteVedtakStegRevurderingFrisinn extends IverksetteVedtakStegTilgrensendeFelles {

    private OpprettProsessTaskIverksett opprettProsessTaskIverksett;
    private ProsessTaskRepository prosessTaskRepository;

    IverksetteVedtakStegRevurderingFrisinn() {
        // for CDI proxy
    }


    @Inject
    public IverksetteVedtakStegRevurderingFrisinn(BehandlingRepositoryProvider repositoryProvider,
                                                  @FagsakYtelseTypeRef OpprettProsessTaskIverksett opprettProsessTaskIverksett,
                                                  VurderBehandlingerUnderIverksettelse tidligereBehandlingUnderIverksettelse,
                                                  IdentifiserOverlappendeInfotrygdYtelseTjeneste identifiserOverlappendeInfotrygdYtelse,
                                                  IverksetteVedtakStatistikk metrikker, ProsessTaskRepository prosessTaskRepository) {
        super(repositoryProvider, tidligereBehandlingUnderIverksettelse, identifiserOverlappendeInfotrygdYtelse, metrikker);
        this.opprettProsessTaskIverksett = opprettProsessTaskIverksett;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    protected void iverksetter(Behandling behandling) {
        // Workaround: Hold igjen for brev for ugunst Frisinn (ikke implementert)
        var ugunst = behandling
            .getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST)
            .isPresent();
        if (ugunst) {
            opprettForsinkelseTaskFørIverksettingstasker(behandling);
            return;
        }
        opprettProsessTaskIverksett.opprettIverksettingstasker(behandling);
    }

    private void opprettForsinkelseTaskFørIverksettingstasker(Behandling behandling) {
        var taskdata = new ProsessTaskData(HoldIgjenIverksettelseTask.TASKTYPE);
        taskdata.setBehandling(behandling.getFagsak().getSaksnummer().getVerdi(),
            String.valueOf(behandling.getId()),
            behandling.getFagsak().getAktørId().getId());
        var forsinkelse = LocalDateTime.now().plusDays(14);
        taskdata.setNesteKjøringEtter(forsinkelse);
        prosessTaskRepository.lagre(taskdata);
    }
}
