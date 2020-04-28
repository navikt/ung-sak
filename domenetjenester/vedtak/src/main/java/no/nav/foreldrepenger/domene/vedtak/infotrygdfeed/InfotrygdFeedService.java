package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
public class InfotrygdFeedService {

    private ProsessTaskRepository prosessTaskRepository;

    private Instance<InfotrygdFeedPeriodeberegner> periodeBeregnere;

    public InfotrygdFeedService() {
        // for CDI
    }

    @Inject
    public InfotrygdFeedService(ProsessTaskRepository prosessTaskRepository,
                                @Any Instance<InfotrygdFeedPeriodeberegner> periodeBeregnere) {
        this.prosessTaskRepository = prosessTaskRepository;
        this.periodeBeregnere = periodeBeregnere;
    }

    private static String tallMedPrefiks(long versjon, int antallSiffer) {
        if (versjon >= Math.pow(10, antallSiffer)) {
            throw new IllegalArgumentException("Versjonsnummeret er for stort");
        }
        return StringUtils.leftPad(Long.toString(versjon), antallSiffer, '0');
    }

    public void publiserHendelse(Behandling behandling) {
        if(FagsakYtelseType.FRISINN.equals(behandling.getFagsakYtelseType())) {
            return;
        }

        validerInput(behandling);

        InfotrygdFeedMessage infotrygdFeedMessage = getInfotrygdFeedMessage(behandling);
        ProsessTaskData pd = getProsessTaskData(behandling, infotrygdFeedMessage);
        prosessTaskRepository.lagre(pd);
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

    private InfotrygdFeedMessage getInfotrygdFeedMessage(Behandling behandling) {
        InfotrygdFeedMessage.Builder builder = InfotrygdFeedMessage.builder()
            .uuid(UUID.randomUUID().toString());

        setSaksnummerOgAktørId(builder, behandling.getFagsak());
        setPeriodeOgYtelse(builder, behandling);
        setAktørIdPleietrengende(builder, behandling);

        return builder.build();
    }

    private void setSaksnummerOgAktørId(InfotrygdFeedMessage.Builder builder, Fagsak fagsak) {
        builder
            .saksnummer(fagsak.getSaksnummer().getVerdi())
            .aktoerId(fagsak.getAktørId().getId());
    }

    private void setPeriodeOgYtelse(InfotrygdFeedMessage.Builder builder, Behandling behandling) {
        InfotrygdFeedPeriodeberegner periodeBeregner = getInfotrygdFeedPeriodeBeregner(behandling);
        builder.ytelse(periodeBeregner.getInfotrygdYtelseKode());

        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();
        InfotrygdFeedPeriode periode = periodeBeregner.finnInnvilgetPeriode(saksnummer);

        LocalDate fom = periode.getFom();
        LocalDate tom = periode.getTom();

        if (!Objects.equals(Tid.TIDENES_BEGYNNELSE, fom)) {
            builder.foersteStoenadsdag(fom);
        }
        if (!Objects.equals(Tid.TIDENES_ENDE, tom)) {
            builder.sisteStoenadsdag(tom);
        }
    }

    private void setAktørIdPleietrengende(InfotrygdFeedMessage.Builder builder, Behandling behandling) {
        AktørId pleietrengendeAktørId = behandling.getFagsak().getPleietrengendeAktørId();
        if (pleietrengendeAktørId != null) {
            builder.aktoerIdPleietrengende(pleietrengendeAktørId.getId());
        }
    }

    private ProsessTaskData getProsessTaskData(Behandling behandling, InfotrygdFeedMessage infotrygdFeedMessage) {
        String tasktype = PubliserInfotrygdFeedElementTask.TASKTYPE;
        ProsessTaskData pd = new ProsessTaskData(tasktype);

        Fagsak fagsak = behandling.getFagsak();

        String saksnummer = fagsak.getSaksnummer().getVerdi();
        String gruppe = tasktype + "-" + saksnummer;
        pd.setGruppe(gruppe);
        pd.setSekvens(lagSekvensnummer(behandling));
        pd.setProperty(PubliserInfotrygdFeedElementTask.KAFKA_KEY_PROPERTY, saksnummer);

        pd.setPayload(infotrygdFeedMessage.toJson());
        return pd;
    }

    private InfotrygdFeedPeriodeberegner getInfotrygdFeedPeriodeBeregner(Behandling behandling) {
        FagsakYtelseType ytelseType = behandling.getFagsak().getYtelseType();

        return FagsakYtelseTypeRef.Lookup.find(periodeBeregnere, ytelseType)
            .orElseThrow(() -> new IllegalArgumentException("Kan ikke beregne periode for ytelse: " + ytelseType));
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
