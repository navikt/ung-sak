package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlag;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.MedisinskGrunnlagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medisinsk.Pleietrengende;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiebehov.PleiebehovResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiebehov.PleiebehovResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.pleiebehov.Pleieperiode;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.uttak.Tid;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

@ApplicationScoped
public class InfotrygdFeedService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MedisinskGrunnlagRepository medisinskGrunnlagRepository;
    private final PleiebehovResultatRepository pleiebehovResultatRepository;
    private final ProsessTaskRepository prosessTaskRepository;

    @Inject
    public InfotrygdFeedService(
        MedisinskGrunnlagRepository medisinskGrunnlagRepository,
        PleiebehovResultatRepository pleiebehovResultatRepository,
        ProsessTaskRepository prosessTaskRepository
    ) {
        this.medisinskGrunnlagRepository = medisinskGrunnlagRepository;
        this.pleiebehovResultatRepository = pleiebehovResultatRepository;
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
        PleiebehovResultat pleiebehovResultat = pleiebehovResultatRepository.hent(behandling.getId());
        MedisinskGrunnlag medisinskGrunnlag = medisinskGrunnlagRepository.hent(behandling.getId());

        InfotrygdFeedMessage.Builder builder = InfotrygdFeedMessage.builder()
            .uuid(UUID.randomUUID().toString());

        setSaksnummerOgAktørId(builder, behandling.getFagsak());
        setFomTom(builder, pleiebehovResultat);
        setAktørIdPleietrengende(builder, medisinskGrunnlag);

        return builder.build();
    }

    private ProsessTaskData getProsessTaskData(Behandling behandling, InfotrygdFeedMessage infotrygdFeedMessage) {
        // todo: legg til asserts på ugyldige verdier
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

    private void setSaksnummerOgAktørId(InfotrygdFeedMessage.Builder builder, Fagsak fagsak) {
        builder
            .saksnummer(fagsak.getSaksnummer().getVerdi())
            .aktoerId(fagsak.getAktørId().getId());
    }

    private void setFomTom(InfotrygdFeedMessage.Builder builder, PleiebehovResultat pleiebehovResultat) {
        Comparator<LocalDate> localDateComparator = Comparator.comparing(d -> d);

        List<DatoIntervallEntitet> perioder = pleiebehovResultat.getPleieperioder().getPerioder().stream()
            .map(Pleieperiode::getPeriode).collect(Collectors.toList());

        LocalDate fom = perioder.stream()
            .map(DatoIntervallEntitet::getFomDato)
            .min(localDateComparator)
            .filter(d -> !Objects.equals(Tid.TIDENES_BEGYNNELSE, d))
            .orElse(null);

        LocalDate tom = perioder.stream()
            .map(DatoIntervallEntitet::getTomDato)
            .max(localDateComparator)
            .filter(d -> !Objects.equals(Tid.TIDENES_ENDE, d))
            .orElse(null);

        builder
            .foersteStoenadsdag(fom)
            .sisteStoenadsdag(tom);
    }

    private void setAktørIdPleietrengende(InfotrygdFeedMessage.Builder builder, MedisinskGrunnlag medisinskGrunnlag) {
        if(medisinskGrunnlag == null) {
            return;
        }
        Pleietrengende pleietrengende = medisinskGrunnlag.getPleietrengende();
        if(pleietrengende == null) {
            return;
        }
        builder.aktoerIdPleietrengende(pleietrengende.getAktørId().getId());
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
