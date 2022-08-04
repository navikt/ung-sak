package no.nav.folketrygdloven.beregningsgrunnlag.regelinput;

import java.net.URI;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusRestKlient;
import no.nav.folketrygdloven.kalkulus.request.v1.regelinput.KomprimerRegelInputRequest;
import no.nav.k9.felles.integrasjon.rest.SystemUserOidcRestClient;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.FagsakProsessTask;

/**
 * Task som kaller kalkulus for gitt saksnummer og komprimerer input for regelsporing dersom dette er mulig.
 *
 *  Kalkulus returerer et forslag til et nytt saksnummer med regelinput som kan komprimeres. Det opprettes så en ny task for dette saksnummeret.
 */
@ApplicationScoped
@ProsessTask(KomprimerRegelInputTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class KomprimerRegelInputTask extends FagsakProsessTask {

    public static final String TASKTYPE = "beregning.komprimerRegelInput";
    private final KalkulusRestKlient kalkulusSystemRestKlient;
    private final EntityManager entityManager;

    @Inject
    public KomprimerRegelInputTask(SystemUserOidcRestClient systemUserOidcRestClient,
                                   @KonfigVerdi(value = "ftkalkulus.url") URI endpoint,
                                   EntityManager entityManager) {
        this.entityManager = entityManager;
        this.kalkulusSystemRestKlient = new KalkulusRestKlient(systemUserOidcRestClient, endpoint);
    }


    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var saksnummer = prosessTaskData.getSaksnummer();
        var nesteSaksnummer = kalkulusSystemRestKlient.komprimerRegelinput(new KomprimerRegelInputRequest(saksnummer));
        if (nesteSaksnummer != null) {
            startInputKomprimeringForSak(nesteSaksnummer.getVerdi());
        }
    }

    private Long startInputKomprimeringForSak(String saksnummer) {
        Objects.requireNonNull(saksnummer);
        String sql = "insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere) " +
            "values (nextval('seq_prosess_task'), 'beregning.komprimerRegelInput', nextval('seq_prosess_task_gruppe'), null, 'saksnummer'=:saksnummer)";
        var query = entityManager.createNativeQuery(sql); // NOSONAR
        query.setParameter("saksnummer", Objects.requireNonNull(saksnummer, "saksnummer"));
        return Integer.toUnsignedLong(query.executeUpdate());
    }

}
