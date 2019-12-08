package no.nav.foreldrepenger.web.app.tjenester.hendelser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.hendelser.HendelseSorteringRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.HendelsemottakRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.kontrakter.abonnent.infotrygd.InfotrygdHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.infotrygd.InfotrygdHendelseDtoBuilder;
import no.nav.foreldrepenger.kontrakter.abonnent.tps.DødfødselHendelseDto;
import no.nav.foreldrepenger.kontrakter.abonnent.tps.FødselHendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.HendelseSorteringTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;
import no.nav.foreldrepenger.mottak.hendelser.KlargjørHendelseTask;
import no.nav.foreldrepenger.mottak.hendelser.MottattHendelseTjeneste;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.DødfødselHendelse;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.FødselHendelse;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.YtelseHendelse;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.HendelserRestTjeneste.AbacAktørIdDto;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.HendelserRestTjeneste.AbacHendelseWrapperDto;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.impl.DødfødselForretningshendelseRegistrerer;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.impl.ForretningshendelseRegistrererProvider;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.impl.FødselForretningshendelseRegistrerer;
import no.nav.foreldrepenger.web.app.tjenester.hendelser.impl.InfotrygdHendelseRegistrerer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;
import no.nav.vedtak.felles.prosesstask.impl.ProsessTaskRepositoryImpl;

public class HendelserRestTjenesteTest {

    private static final String HENDELSE_ID = "1337";

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private HendelsemottakRepository hendelsemottakRepository = new HendelsemottakRepository(repoRule.getEntityManager());
    private ProsessTaskRepository prosessTaskRepository = new ProsessTaskRepositoryImpl(repoRule.getEntityManager(), null);
    private HendelseSorteringRepository sorteringRepository = mock(HendelseSorteringRepository.class);
    private HendelserRestTjeneste hendelserRestTjeneste;

    @Before
    public void before() {
        MottattHendelseTjeneste mottattHendelseTjeneste = new MottattHendelseTjeneste(hendelsemottakRepository, prosessTaskRepository);
        HendelseSorteringTjeneste sorteringTjeneste = new HendelseSorteringTjeneste(sorteringRepository);

        FødselForretningshendelseRegistrerer fødselRegistrerer = new FødselForretningshendelseRegistrerer(mottattHendelseTjeneste);
        DødfødselForretningshendelseRegistrerer dødfødselRegistrerer = new DødfødselForretningshendelseRegistrerer(mottattHendelseTjeneste);
        InfotrygdHendelseRegistrerer infotrygdRegistrerer = new InfotrygdHendelseRegistrerer(mottattHendelseTjeneste);
        ForretningshendelseRegistrererProvider provider = mock(ForretningshendelseRegistrererProvider.class);
        doReturn(fødselRegistrerer).when(provider).finnRegistrerer(any(FødselHendelseDto.class));
        doReturn(dødfødselRegistrerer).when(provider).finnRegistrerer(any(DødfødselHendelseDto.class));
        doReturn(infotrygdRegistrerer).when(provider).finnRegistrerer(any(InfotrygdHendelseDto.class));

        hendelserRestTjeneste = new HendelserRestTjeneste(mottattHendelseTjeneste, sorteringTjeneste, provider);
    }

    @Test
    public void skal_ta_imot_fødselshendelse_og_opprette_prosesstask() {
        // Arrange
        List<AktørId> aktørIdForeldre = List.of(AktørId.dummy(), AktørId.dummy());
        LocalDate fødselsdato = LocalDate.now();

        // Act
        hendelserRestTjeneste.mottaHendelse(new AbacHendelseWrapperDto(lagFødselHendelse(aktørIdForeldre, fødselsdato)));
        repoRule.getEntityManager().flush();

        // Assert
        assertThat(hendelsemottakRepository.hendelseErNy(HENDELSE_ID)).isFalse();
        List<ProsessTaskData> tasks = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        ProsessTaskData task = tasks.stream().filter(d -> Objects.equals(KlargjørHendelseTask.TASKTYPE, d.getTaskType())).findFirst().orElseThrow();
        assertThat(task.getTaskType()).isEqualTo(KlargjørHendelseTask.TASKTYPE);
        assertThat(task.getPayloadAsString()).isEqualTo(JsonMapper.toJson(new FødselHendelse(aktørIdForeldre.stream().map(AktørId::getId).collect(Collectors.toList()), fødselsdato)));
        assertThat(task.getPropertyValue(KlargjørHendelseTask.PROPERTY_UID)).isEqualTo(HENDELSE_ID);
        assertThat(task.getPropertyValue(KlargjørHendelseTask.PROPERTY_HENDELSE_TYPE)).isEqualTo("FØDSEL");
    }

    @Test
    public void skal_ta_imot_dødfødselhendelse_og_opprette_prosesstask() {
        // Arrange
        List<AktørId> aktørIdForeldre = List.of(AktørId.dummy(), AktørId.dummy());
        LocalDate dødfødseldato = LocalDate.now();

        // Act
        hendelserRestTjeneste.mottaHendelse(new AbacHendelseWrapperDto(lagDødfødselHendelse(aktørIdForeldre, dødfødseldato)));
        repoRule.getEntityManager().flush();

        // Assert
        assertThat(hendelsemottakRepository.hendelseErNy(HENDELSE_ID)).isFalse();
        List<ProsessTaskData> tasks = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        ProsessTaskData task = tasks.stream().filter(d -> Objects.equals(KlargjørHendelseTask.TASKTYPE, d.getTaskType())).findFirst().orElseThrow();
        assertThat(task.getTaskType()).isEqualTo(KlargjørHendelseTask.TASKTYPE);
        assertThat(task.getPayloadAsString()).isEqualTo(JsonMapper.toJson(new DødfødselHendelse(aktørIdForeldre.stream().map(AktørId::getId).collect(Collectors.toList()), dødfødseldato)));
        assertThat(task.getPropertyValue(KlargjørHendelseTask.PROPERTY_UID)).isEqualTo(HENDELSE_ID);
        assertThat(task.getPropertyValue(KlargjørHendelseTask.PROPERTY_HENDELSE_TYPE)).isEqualTo("DØDFØDSEL");
    }

    @Test
    public void skal_ta_imot_infotrygdhendelse_og_opprette_prosesstask() {
        // Arrange
        LocalDate fom = LocalDate.now();
        String identDato = "2018-10-10";
        String aktørId = "900000001";
        String typeYtelse = "ab";
        String hendelseKode = InfotrygdHendelseDto.Hendelsetype.YTELSE_ENDRET.name();
        InfotrygdHendelseDto infotrygdDto = lagInfotrygdHendelse(InfotrygdHendelseDto.Hendelsetype.YTELSE_ENDRET,
            aktørId,
            typeYtelse,
            fom,
            identDato);

        // Act
        hendelserRestTjeneste.mottaHendelse(new AbacHendelseWrapperDto(infotrygdDto));
        repoRule.getEntityManager().flush();

        // Assert
        assertThat(hendelsemottakRepository.hendelseErNy(HENDELSE_ID)).isFalse();
        List<ProsessTaskData> tasks = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        ProsessTaskData task = tasks.stream().filter(d -> Objects.equals(KlargjørHendelseTask.TASKTYPE, d.getTaskType())).findFirst().orElseThrow();
        assertThat(task.getTaskType()).isEqualTo(KlargjørHendelseTask.TASKTYPE);
        assertThat(task.getPayloadAsString()).isEqualTo(JsonMapper.toJson(new YtelseHendelse(hendelseKode, typeYtelse, aktørId, fom, identDato)));
        assertThat(task.getPropertyValue(KlargjørHendelseTask.PROPERTY_UID)).isEqualTo(HENDELSE_ID);
        assertThat(task.getPropertyValue(KlargjørHendelseTask.PROPERTY_HENDELSE_TYPE)).isEqualTo(hendelseKode);
    }

    @Test
    public void skal_ikke_opprette_prosess_task_når_hendelse_med_samme_uid_tidligere_er_mottatt() {
        // Arrange
        hendelsemottakRepository.registrerMottattHendelse(HENDELSE_ID);
        List<AktørId> aktørIdForeldre = List.of(AktørId.dummy(), AktørId.dummy());
        LocalDate fødselsdato = LocalDate.now();

        // Act
        hendelserRestTjeneste.mottaHendelse(new AbacHendelseWrapperDto(lagFødselHendelse(aktørIdForeldre, fødselsdato)));
        repoRule.getEntityManager().flush();

        // Assert
        List<ProsessTaskData> tasks = prosessTaskRepository.finnAlle(ProsessTaskStatus.KLAR);
        assertThat(tasks).allSatisfy(d -> assertThat(d.getTaskType()).isNotEqualTo(KlargjørHendelseTask.TASKTYPE));
    }

    @Test
    public void skal_returnere_tom_liste_når_aktørId_ikke_er_registrert_eller_mangler_sak() {
        // Arrange
        when(sorteringRepository.hentEksisterendeAktørIderMedSak(anyList())).thenReturn(Collections.emptyList());

        // Act
        List<String> resultat = hendelserRestTjeneste.grovSorter(List.of(new AbacAktørIdDto("3512880731200")));

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_returnere_liste_med_4_aktørIder_som_har_sak() {
        // Arrange
        List<AktørId> harSak = new ArrayList<>(List.of(
            AktørId.dummy(),
            AktørId.dummy(),
            AktørId.dummy(),
            AktørId.dummy()));

        when(sorteringRepository.hentEksisterendeAktørIderMedSak(anyList())).thenReturn(harSak);

        List<AbacAktørIdDto> sorter = new ArrayList<>();
        sorter.add(new AbacAktørIdDto("3512880731200"));
        sorter.add(new AbacAktørIdDto("4512880731201"));
        sorter.add(new AbacAktørIdDto("5512880731202"));
        sorter.add(new AbacAktørIdDto("6512880731203"));
        sorter.add(new AbacAktørIdDto("7512880731204"));
        sorter.add(new AbacAktørIdDto("8512880731205"));
        sorter.add(new AbacAktørIdDto("9512880731206"));

        // Act
        List<String> resultat = hendelserRestTjeneste.grovSorter(sorter);

        // Assert
        assertThat(resultat).hasSameSizeAs(harSak);
        assertThat(resultat).isEqualTo(harSak.stream().map(AktørId::getId).collect(Collectors.toList()));
    }

    private FødselHendelseDto lagFødselHendelse(List<AktørId> aktørIdForeldre, LocalDate fødselsdato) {
        FødselHendelseDto hendelse = new FødselHendelseDto();
        hendelse.setId(HENDELSE_ID);
        hendelse.setAktørIdForeldre(aktørIdForeldre.stream().map(AktørId::getId).collect(Collectors.toList()));
        hendelse.setFødselsdato(fødselsdato);
        return hendelse;
    }

    private DødfødselHendelseDto lagDødfødselHendelse(List<AktørId> aktørIdForeldre, LocalDate dødfødseldato) {
        DødfødselHendelseDto hendelse = new DødfødselHendelseDto();
        hendelse.setId(HENDELSE_ID);
        hendelse.setAktørId(aktørIdForeldre.stream().map(AktørId::getId).collect(Collectors.toList()));
        hendelse.setDødfødselsdato(dødfødseldato);
        return hendelse;
    }

    private InfotrygdHendelseDto lagInfotrygdHendelse(InfotrygdHendelseDto.Hendelsetype hendelsetype,
                                                      String aktørId,
                                                      String typeYtelse,
                                                      LocalDate fom,
                                                      String identDato) {
        InfotrygdHendelseDtoBuilder builder;
        switch (hendelsetype) {
            case YTELSE_OPPHØRT:
                builder = InfotrygdHendelseDtoBuilder.opphørt();
                break;
            case YTELSE_ENDRET:
                builder = InfotrygdHendelseDtoBuilder.endring();
                break;
            case YTELSE_ANNULERT:
                builder = InfotrygdHendelseDtoBuilder.annulert();
                break;
            case YTELSE_INNVILGET:
                builder = InfotrygdHendelseDtoBuilder.innvilget();
                break;
            default:
                throw new IllegalArgumentException("ugyldig hendelsetype");
        }
        return builder.medUnikId(HENDELSE_ID)
            .medAktørId(aktørId)
            .medFraOgMed(fom)
            .medIdentdato(identDato)
            .medTypeYtelse(typeYtelse)
            .build();
    }
}
