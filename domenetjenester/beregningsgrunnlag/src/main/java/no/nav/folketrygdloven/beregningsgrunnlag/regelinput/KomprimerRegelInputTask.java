package no.nav.folketrygdloven.beregningsgrunnlag.regelinput;

import java.net.URI;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusRestKlient;
import no.nav.folketrygdloven.kalkulus.request.v1.regelinput.KomprimerRegelInputRequest;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.task.FagsakProsessTask;
import no.nav.k9.sak.typer.Saksnummer;

/**
 * Task som kaller kalkulus for gitt saksnummer og komprimerer input for regelsporing dersom dette er mulig.
 * <p>
 * Kalkulus returerer et forslag til et nytt saksnummer med regelinput som kan komprimeres. Det opprettes så en ny task for dette saksnummeret.
 */
@ApplicationScoped
@ProsessTask(KomprimerRegelInputTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class KomprimerRegelInputTask extends FagsakProsessTask {

    public static final String TASKTYPE = "beregning.komprimerRegelInput";
    public static final String MAX_ANTALL_SAKER = "maxAntallSaker";
    public static final String LØPENR_SAK = "lopenr_sak";

    private KalkulusRestKlient kalkulusSystemRestKlient;
    private FagsakRepository fagsakRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    public KomprimerRegelInputTask(FagsakLåsRepository fagsakLåsRepository,
                                   BehandlingLåsRepository behandlingLåsRepository,
                                   SystemUserOidcRestClient systemUserOidcRestClient,
                                   @KonfigVerdi(value = "ftkalkulus.url") URI endpoint,
                                   FagsakRepository fagsakRepository,
                                   ProsessTaskTjeneste prosessTaskTjeneste) {
        super(fagsakLåsRepository, behandlingLåsRepository);
        this.fagsakRepository = fagsakRepository;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.kalkulusSystemRestKlient = new KalkulusRestKlient(systemUserOidcRestClient, endpoint);
    }


    public KomprimerRegelInputTask() {
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var saksnummer = prosessTaskData.getSaksnummer();
        var maksAntalLSakerRaw = prosessTaskData.getPropertyValue(MAX_ANTALL_SAKER);
        var maksAntallSaker = prosessTaskData.getPropertyValue(MAX_ANTALL_SAKER) == null ? null : Integer.valueOf(maksAntalLSakerRaw);
        var lopenr = prosessTaskData.getPropertyValue(LØPENR_SAK) == null ? 1 : Integer.parseInt(prosessTaskData.getPropertyValue(LØPENR_SAK));
        var nesteSaksnummer = kalkulusSystemRestKlient.komprimerRegelinput(new KomprimerRegelInputRequest(saksnummer));
        if (nesteSaksnummer != null && (maksAntallSaker == null || lopenr < maksAntallSaker)) {
            var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(nesteSaksnummer));
            fagsak.ifPresent(f -> startInputKomprimeringForSak(f, lopenr, maksAntallSaker));
        }
    }

    private void startInputKomprimeringForSak(Fagsak fagsak, Integer lopenr, Integer maksAntallSaker) {
        Objects.requireNonNull(fagsak);
        var saksnummer = fagsak.getSaksnummer().getVerdi();
        ProsessTaskData nesteTask = ProsessTaskData.forProsessTask(KomprimerRegelInputTask.class);
        nesteTask.setFagsakId(fagsak.getId());
        nesteTask.setSaksnummer(saksnummer);
        if (maksAntallSaker != null) {
            nesteTask.setProperty(MAX_ANTALL_SAKER, maksAntallSaker.toString());
        }
        int nesteLopenr = lopenr + 1;
        nesteTask.setProperty(LØPENR_SAK, Integer.toString(nesteLopenr));
        prosessTaskTjeneste.lagre(nesteTask);
    }

}
