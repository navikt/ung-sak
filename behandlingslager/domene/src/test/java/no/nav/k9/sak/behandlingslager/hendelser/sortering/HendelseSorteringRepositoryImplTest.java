package no.nav.k9.sak.behandlingslager.hendelser.sortering;

import static java.time.Month.JANUARY;
import static java.util.Collections.singletonList;
import static no.nav.k9.kodeverk.person.NavBrukerKjønn.KVINNE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.hendelser.HendelseSorteringRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class HendelseSorteringRepositoryImplTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    public Repository repository = repositoryRule.getRepository();

    private HendelseSorteringRepository sorteringRepository = new HendelseSorteringRepository(repositoryRule.getEntityManager());
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private PersonopplysningRepository personopplysningRepository = repositoryProvider.getPersonopplysningRepository();


    @Test
    public void skal_hente_1_aktørId_fra_fagsak() {
        var personer = genererFagsaker(1);

        AktørId aktørId = personer.get(0).getAktørId();

        List<AktørId> finnAktørIder = List.of(aktørId);
        List<AktørId> resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);

        assertThat(resultat).isNotEmpty();
        assertThat(resultat).containsExactly(aktørId);
    }

    @Test
    public void skal_returnere_tom_liste_når_aktør_id_ikke_er_knyttet_til_sak() {
        // setup
        @SuppressWarnings("unused")
        var personer = genererFagsaker(4); // aktør ID: 100 - 103

        List<AktørId> finnAktørIder = List.of(AktørId.dummy());

        // act
        List<AktørId> resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);

        // assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_returnere_4_aktør_ider_fra_fagsaker() {
        var personer = genererFagsaker(6);

        List<AktørId> finnAktørIder = personer.stream().map(Personinfo::getAktørId).limit(4).collect(Collectors.toList());

        List<AktørId> resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);
        assertThat(resultat).hasSize(4);
    }

    @Test
    public void skal_ikke_publisere_videre_hendelser_på_avsluttede_saker() {
        List<Personinfo> personinfoList = genererPersonInfo(3);

        var aktørId = personinfoList.get(0).getAktørId();
        Fagsak fagsak1 = opprettFagsak(aktørId, FagsakYtelseType.FORELDREPENGER);
        fagsak1.setAvsluttet();

        var aktørId1 = personinfoList.get(1).getAktørId();
        Fagsak fagsak2 = opprettFagsak(aktørId1, FagsakYtelseType.FORELDREPENGER);

        var aktørId2 = personinfoList.get(2).getAktørId();
        Fagsak fagsak3 = opprettFagsak(aktørId2, FagsakYtelseType.FORELDREPENGER);
        Fagsak fagsak4 = opprettFagsak(aktørId2, FagsakYtelseType.FORELDREPENGER);
        fagsak4.setAvsluttet();

        repository.lagre(fagsak1);
        repository.lagre(fagsak2);
        repository.lagre(fagsak3);
        repository.lagre(fagsak4);
        repository.flushAndClear();

        List<AktørId> aktørList = personinfoList.stream().map(Personinfo::getAktørId).collect(Collectors.toList());
        List<AktørId> resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(aktørList);

        assertThat(resultat).hasSize(2);
        assertThat(resultat).contains(aktørId1);
        assertThat(resultat).contains(aktørId2);
    }

    @Test
    public void skal_publisere_videre_hendelser_på_saker_om_engangsstønad() {
        // Arrange
        List<Personinfo> personinfoList = genererPersonInfo(1);
        var aktørId = personinfoList.get(0).getAktørId();
        Fagsak fagsak = opprettFagsak(aktørId, FagsakYtelseType.ENGANGSTØNAD);

        repository.lagre(fagsak);
        repository.flushAndClear();

        List<AktørId> finnAktørIder = List.of(aktørId);

        // Act
        List<AktørId> resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);

        // Assert
        assertThat(resultat).contains(aktørId);
    }

    @Test
    public void skal_publisere_videre_hendelser_på_saker_om_svangerskapspenger() {
        // Arrange
        List<Personinfo> personinfoList = genererPersonInfo(1);
        var aktørId = personinfoList.get(0).getAktørId();
        Fagsak fagsak = opprettFagsak(aktørId, FagsakYtelseType.SVANGERSKAPSPENGER);

        repository.lagre(fagsak);
        repository.flushAndClear();

        List<AktørId> finnAktørIder = List.of(aktørId);

        // Act
        List<AktørId> resultat = sorteringRepository.hentEksisterendeAktørIderMedSak(finnAktørIder);

        // Assert
        assertThat(resultat).contains(aktørId);
    }

    @Test
    public void skal_finne_match_på_både_mor_og_barn_i_behandlingsgrunnlaget() {
        // Arrange
        AktørId barnAktørId = AktørId.dummy();
        LocalDate fødselsdato = LocalDate.now();

        List<Personinfo> personinfoList = genererPersonInfo(1);
        AktørId morAktørId = personinfoList.get(0).getAktørId();
        Fagsak fagsak = opprettFagsak(morAktørId, FagsakYtelseType.FORELDREPENGER);
        repository.lagre(fagsak);
        repository.flushAndClear();

        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = behandlingBuilder.build();
        repository.lagre(behandling);
        repository.flushAndClear();

        Long behandlingId = behandling.getId();
        PersonInformasjonBuilder informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        informasjonBuilder
            .leggTil(
                informasjonBuilder.getPersonopplysningBuilder(barnAktørId)
                    .medKjønn(NavBrukerKjønn.MANN)
                    .medNavn("Barn Hansen")
                    .medFødselsdato(fødselsdato))
            .leggTil(
                informasjonBuilder.getPersonopplysningBuilder(morAktørId)
                    .medKjønn(NavBrukerKjønn.KVINNE)
                    .medNavn("Mor Hansen")
                    .medFødselsdato(fødselsdato.minusYears(25)))
            .leggTil(
                informasjonBuilder
                    .getRelasjonBuilder(morAktørId, barnAktørId, RelasjonsRolleType.BARN))
            .leggTil(
                informasjonBuilder
                    .getRelasjonBuilder(barnAktørId, morAktørId, RelasjonsRolleType.MORA));

        personopplysningRepository.lagre(behandlingId, informasjonBuilder);

        // Act
        List<AktørId> resultat1 = sorteringRepository.hentEksisterendeAktørIderMedSak(singletonList(morAktørId));
        List<AktørId> resultat2 = sorteringRepository.hentEksisterendeAktørIderMedSak(singletonList(barnAktørId));

        // Assert
        assertThat(resultat1).hasSize(1);
        assertThat(resultat1).contains(morAktørId);
        assertThat(resultat2).hasSize(1);
        assertThat(resultat2).contains(barnAktørId);
    }

    @Test
    public void skal_ikke_finne_match_på_barn_i_behandlingsgrunnlaget_når_relasjonen_mangler() {
        // Arrange
        AktørId barnAktørId = AktørId.dummy();
        LocalDate fødselsdato = LocalDate.now();

        List<Personinfo> personinfoList = genererPersonInfo(1);
        AktørId morAktørId = personinfoList.get(0).getAktørId();

        Fagsak fagsak = opprettFagsak(morAktørId, FagsakYtelseType.FORELDREPENGER);
        repository.lagre(fagsak);
        repository.flushAndClear();

        Behandling.Builder behandlingBuilder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = behandlingBuilder.build();
        repository.lagre(behandling);
        repository.flushAndClear();

        Long behandlingId = behandling.getId();
        PersonInformasjonBuilder informasjonBuilder = personopplysningRepository.opprettBuilderForRegisterdata(behandlingId);
        informasjonBuilder
            .leggTil(
                informasjonBuilder.getPersonopplysningBuilder(barnAktørId)
                    .medKjønn(NavBrukerKjønn.MANN)
                    .medNavn("Barn Hansen")
                    .medFødselsdato(fødselsdato))
            .leggTil(
                informasjonBuilder.getPersonopplysningBuilder(morAktørId)
                    .medKjønn(NavBrukerKjønn.KVINNE)
                    .medNavn("Mor Hansen")
                    .medFødselsdato(fødselsdato.minusYears(25)));

        personopplysningRepository.lagre(behandlingId, informasjonBuilder);

        // Act
        List<AktørId> resultat1 = sorteringRepository.hentEksisterendeAktørIderMedSak(singletonList(morAktørId));
        List<AktørId> resultat2 = sorteringRepository.hentEksisterendeAktørIderMedSak(singletonList(barnAktørId));

        // Assert
        assertThat(resultat1).hasSize(1);
        assertThat(resultat1).contains(morAktørId);
        assertThat(resultat2).isEmpty();
    }

    private Fagsak opprettFagsak(AktørId bruker, FagsakYtelseType fagsakYtelseType) {
        return Fagsak.opprettNy(fagsakYtelseType, bruker);
    }

    private List<Personinfo> genererFagsaker(int antall) {
        List<Personinfo> personinfoList = genererPersonInfo(antall);

        List<Fagsak> fagsaker = new ArrayList<>();

        for (Personinfo pInfo : personinfoList) {
            fagsaker.add(opprettFagsak(pInfo.getAktørId(), FagsakYtelseType.FORELDREPENGER));
        }

        if (!fagsaker.isEmpty()) {
            repository.lagre(fagsaker);
            repository.flushAndClear();
        }

        return personinfoList;
    }

    private List<Personinfo> genererPersonInfo(int antall) {
        String fnr = "123456678901";

        List<Personinfo> personinfoList = new ArrayList<>();
        for (int i = 0; i < antall; i++) {
            personinfoList.add(
                new Personinfo.Builder()
                    .medAktørId(AktørId.dummy())
                    .medPersonIdent(new PersonIdent(fnr))
                    .medNavn("Kari Nordmann")
                    .medFødselsdato(LocalDate.of(1990, JANUARY, 1))
                    .medKjønn(KVINNE)
                    .medForetrukketSpråk(Språkkode.nb)
                    .build()
            );
        }
        return personinfoList;
    }
}
