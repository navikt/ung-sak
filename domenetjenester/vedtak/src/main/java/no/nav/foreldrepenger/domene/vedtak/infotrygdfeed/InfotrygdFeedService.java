package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import org.apache.commons.lang3.StringUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

@ApplicationScoped
public class InfotrygdFeedService {

    private ProsessTaskTjeneste taskTjeneste;

    public InfotrygdFeedService() {
        // for CDI
    }

    @Inject
    public InfotrygdFeedService(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    private static String tallMedPrefiks(long versjon, int antallSiffer) {
        if (versjon >= Math.pow(10, antallSiffer)) {
            throw new IllegalArgumentException("Versjonsnummeret er for stort");
        }
        return StringUtils.leftPad(Long.toString(versjon), antallSiffer, '0');
    }

    public void publiserHendelse(Behandling behandling) {

        // FIXME FagsakYtelseType: Ikke branch på ytelsetype i generelle moduler
        if (FagsakYtelseType.FRISINN.equals(behandling.getFagsakYtelseType())) {
            return;
        }
        if (BehandlingResultatType.AVSLÅTT.equals(behandling.getBehandlingResultatType())) {
            return;
        }

        validerInput(behandling);

        ProsessTaskData pd = getProsessTaskData(behandling);
        taskTjeneste.lagre(pd);
    }

    private void validerInput(Behandling behandling) {
        if (behandling.getFagsak().getSaksnummer() == null) {
            throw new ManglendeVerdiException("behandling.fagsak.saksnummer");
        }
        if (behandling.getFagsak().getAktørId() == null) {
            throw new ManglendeVerdiException("behandling.fagsak.aktørId");
        }
        if (behandling.getVersjon() == null) {
            throw new ManglendeVerdiException("behandling.versjon");
        }
    }

    private ProsessTaskData getProsessTaskData(Behandling behandling) {
        ProsessTaskData pd = ProsessTaskData.forProsessTask(PubliserInfotrygdFeedElementTask.class);

        Fagsak fagsak = behandling.getFagsak();

        String saksnummer = fagsak.getSaksnummer().getVerdi();
        pd.setSekvens(lagSekvensnummer(behandling));
        pd.setBehandling(behandling.getFagsak().getSaksnummer().getVerdi(), behandling.getId().toString(), behandling.getAktørId().getId());
        pd.setProperty(PubliserInfotrygdFeedElementTask.KAFKA_KEY_PROPERTY, saksnummer);

        pd.setCallIdFraEksisterende();
        return pd;
    }

    private String lagSekvensnummer(Behandling behandling) {
        // Sekvensnummeret må la seg sortere slik at meldingene blir sendt i riktig rekkefølge.

        int antallSiffer = 5; // et vilkårlig antall siffer som er stort nok til å holde på et versjonsnummer
        String sekvensnummerFagsak = tallMedPrefiks(behandling.getFagsak().getVersjon(), antallSiffer);
        String sekvensnummerBehandling = tallMedPrefiks(behandling.getVersjon(), antallSiffer);
        return String.format("%s-%s", sekvensnummerFagsak, sekvensnummerBehandling);
    }

    public static class ManglendeVerdiException extends RuntimeException {
        public ManglendeVerdiException(String verdiSomMangler) {
            super("Mangler verdi for felt: " + verdiSomMangler);
        }
    }
}
