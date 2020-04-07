package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.k9.kodeverk.uttak.UtfallType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.k9.sak.domene.uttak.uttaksplan.Uttaksplan;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

@ApplicationScoped
public class InfotrygdFeedService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final UttakTjeneste uttakTjeneste;
    private final ProsessTaskRepository prosessTaskRepository;

    public InfotrygdFeedService() {
        // for CDI
        uttakTjeneste = null;
        prosessTaskRepository = null;
    }

    @Inject
    public InfotrygdFeedService(
        UttakTjeneste uttakTjeneste, ProsessTaskRepository prosessTaskRepository
    ) {
        this.uttakTjeneste = uttakTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    public void publiserHendelse(Behandling behandling) {
        validerInput(behandling);

        InfotrygdFeedMessage infotrygdFeedMessage = getInfotrygdFeedMessage(behandling);
        ProsessTaskData pd = getProsessTaskData(behandling, infotrygdFeedMessage);
        prosessTaskRepository.lagre(pd);
    }

    private void validerInput(Behandling behandling) {
        if(behandling.getFagsak().getSaksnummer() == null) {
            throw new ManglendeVerdiException("behandling.fagsak.saksnummer");
        }
        if(behandling.getFagsak().getAktørId() == null) {
            throw new ManglendeVerdiException("behandling.fagsak.aktørId");
        }
        if(behandling.getVersjon() == null) {
            throw new ManglendeVerdiException("behandling.versjon");
        }
        if(!Objects.equals(PLEIEPENGER_SYKT_BARN, behandling.getFagsak().getYtelseType())) {
            throw new IllegalArgumentException(String.format("Forventet ytelsestype '%s'. Fikk '%s'.", PLEIEPENGER_SYKT_BARN, behandling.getFagsak().getYtelseType()));
        }
    }

    private InfotrygdFeedMessage getInfotrygdFeedMessage(Behandling behandling) {
        InfotrygdFeedMessage.Builder builder = InfotrygdFeedMessage.builder()
            .ytelse("PN")
            .uuid(UUID.randomUUID().toString());

        setSaksnummerOgAktørId(builder, behandling.getFagsak());
        setFomTom(builder, behandling);
        setAktørIdPleietrengende(builder, behandling);

        return builder.build();
    }

    private void setSaksnummerOgAktørId(InfotrygdFeedMessage.Builder builder, Fagsak fagsak) {
        builder
            .saksnummer(fagsak.getSaksnummer().getVerdi())
            .aktoerId(fagsak.getAktørId().getId());
    }

    private void setFomTom(InfotrygdFeedMessage.Builder builder, Behandling behandling) {
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();
        Map<Saksnummer, Uttaksplan> saksnummerUttaksplanMap = uttakTjeneste.hentUttaksplaner(List.of(saksnummer));
        Uttaksplan uttaksplan = saksnummerUttaksplanMap.get(saksnummer);
        if(uttaksplan == null) {
            logger.info("Ingen treff i uttaksplaner. Antar at saken er annullert. Saksnummer: " + saksnummer);
            return;
        }
        List<Periode> perioder = uttaksplan.getPerioder().entrySet().stream()
            .filter(e -> Objects.equals(UtfallType.INNVILGET, e.getValue().getUtfall()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        LocalDate fom = perioder.stream().map(Periode::getFom).min(Comparator.naturalOrder()).orElse(null);
        LocalDate tom = perioder.stream().map(Periode::getTom).max(Comparator.naturalOrder()).orElse(null);

        if(!Objects.equals(Tid.TIDENES_BEGYNNELSE, fom)) {
            builder.foersteStoenadsdag(fom);
        }
        if(!Objects.equals(Tid.TIDENES_ENDE, tom)) {
            builder.sisteStoenadsdag(tom);
        }
    }

    private void setAktørIdPleietrengende(InfotrygdFeedMessage.Builder builder, Behandling behandling) {
        AktørId pleietrengendeAktørId = behandling.getFagsak().getPleietrengendeAktørId();
        if(pleietrengendeAktørId != null) {
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

    private String lagSekvensnummer(Behandling behandling) {
        // Sekvensnummeret må la seg sortere slik at meldingene blir sendt i riktig rekkefølge.

        int antallSiffer = 5; // et vilkårlig antall siffer som er stort nok til å holde på et versjonsnummer
        String sekvensnummerFagsak = tallMedPrefiks(behandling.getFagsak().getVersjon(), antallSiffer);
        String sekvensnummerBehandling = tallMedPrefiks(behandling.getVersjon(), antallSiffer);
        return String.format("%s-%s", sekvensnummerFagsak, sekvensnummerBehandling);
    }

    private static String tallMedPrefiks(long versjon, int antallSiffer) {
        if(versjon >= Math.pow(10, antallSiffer)) {
            throw new IllegalArgumentException("Versjonsnummeret er for stort");
        }
        return StringUtils.leftPad(Long.toString(versjon), antallSiffer, '0');
    }

    public static class ManglendeVerdiException extends RuntimeException {
        public ManglendeVerdiException(String verdiSomMangler) {
            super("Mangler verdi for felt: " + verdiSomMangler);
        }
    }
}
