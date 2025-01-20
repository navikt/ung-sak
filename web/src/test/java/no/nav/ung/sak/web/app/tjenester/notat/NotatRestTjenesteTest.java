package no.nav.ung.sak.web.app.tjenester.notat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.exception.TekniskException;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.notat.NotatGjelderType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.notat.NotatRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.kontrakt.notat.EndreNotatDto;
import no.nav.ung.sak.kontrakt.notat.NotatDto;
import no.nav.ung.sak.kontrakt.notat.OpprettNotatDto;
import no.nav.ung.sak.kontrakt.notat.SkjulNotatDto;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class NotatRestTjenesteTest {

    @Inject
    private EntityManager entityManager;

    private FagsakRepository fagsakRepository;
    private NotatRepository notatRepository;
    private NotatRestTjeneste notatRestTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;


    @BeforeEach
    void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        notatRepository = new NotatRepository(entityManager);
        notatRestTjeneste = new NotatRestTjeneste(notatRepository, fagsakRepository);
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker("enSaksbehandler");
    }

    @AfterEach
    void tearDown() {
        SubjectHandlerUtils.reset();
    }

    @Test
    void skalOppretteOgHenteNotat() {
        Fagsak fagsak = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OMSORGSPENGER_KS).lagreFagsak(repositoryProvider);
        var saksnummer = fagsak.getSaksnummer();
        String tekst = "en tekst med litt notater";

        var notatDto = new OpprettNotatDto(
                tekst,
                saksnummer,
                NotatGjelderType.FAGSAK
        );

        opprettNotat(notatDto);
        NotatDto notat = hentForFagsak(saksnummer).stream().findFirst().orElseThrow();

        assertThat(notat.notatTekst()).isEqualTo(tekst);
        assertThat(notat.skjult()).isFalse();
        assertThat(notat.opprettetAv()).isEqualTo("enSaksbehandler");
        assertThat(notat.opprettetTidspunkt()).isNotNull();
        assertThat(notat.endretAv()).isNull();
        assertThat(notat.endretTidspunkt()).isNull();

    }

    @Test
    void skalEndreTekst() {
        var mor = AktørId.dummy();
        var morSak = TestScenarioBuilder.builderMedSøknad(mor).lagreFagsak(repositoryProvider);

        String morTekst = "et gammelt notat ";
        var saksnummer = morSak.getSaksnummer();
        opprettNotat(new OpprettNotatDto(
                morTekst,
                saksnummer,
                NotatGjelderType.FAGSAK
        ));

        List<NotatDto> morNotater = hentForFagsak(saksnummer);
        NotatDto morNotat = morNotater.stream().findFirst().orElseThrow();
        assertThat(morNotat.gjelderType()).isEqualTo(NotatGjelderType.FAGSAK);
        assertThat(morNotat.notatTekst()).isEqualTo(morTekst);
        assertThat(morNotat.opprettetTidspunkt()).isNotNull();
        assertThat(morNotat.endretTidspunkt()).isNull();
        assertThat(morNotat.versjon()).isEqualTo(0);
        assertThat(morNotat.kanRedigere()).isTrue();

        var endretTekst = "et endret notat";
        endreNotat(new EndreNotatDto(
                morNotat.notatId(),
                endretTekst,
                saksnummer,
                morNotat.versjon()));

        NotatDto endretNotat = hentNotat(saksnummer, morNotat.notatId());
        assertThat(endretNotat.gjelderType()).isEqualTo(NotatGjelderType.FAGSAK);
        assertThat(endretNotat.notatTekst()).isEqualTo(endretTekst);
        assertThat(endretNotat.opprettetAv()).isEqualTo(morNotat.opprettetAv());
        assertThat(endretNotat.endretTidspunkt()).isAfter(morNotat.opprettetTidspunkt());
        assertThat(endretNotat.kanRedigere()).isTrue();
        assertThat(endretNotat.versjon()).isEqualTo(1);
    }

    @Test
    void skalIkkeEndreAndreSineNotaterTekst() {
        var mor = AktørId.dummy();
        var morSak = TestScenarioBuilder.builderMedSøknad(mor).lagreFagsak(repositoryProvider);

        var saksnummer = morSak.getSaksnummer();

        SubjectHandlerUtils.setInternBruker("saksbehandler1");
        opprettNotat(new OpprettNotatDto(
                "et gammelt notat ",
                saksnummer,
                NotatGjelderType.FAGSAK
        ));

        SubjectHandlerUtils.setInternBruker("saksbehandler2");
        List<NotatDto> morNotater = hentForFagsak(saksnummer);
        NotatDto morNotat = morNotater.stream().findFirst().orElseThrow();

        assertThat(morNotat.kanRedigere()).isFalse();

        assertThatThrownBy(() -> endreNotat(new EndreNotatDto(
                morNotat.notatId(),
                "et endret notat",
                saksnummer,
                morNotat.versjon()))).isInstanceOf(ManglerTilgangException.class);

    }


    @Test
    void skalFeileHvisEndrerPåEldreVersjon() {
        var mor = AktørId.dummy();
        var morSak = TestScenarioBuilder.builderMedSøknad(mor).lagreFagsak(repositoryProvider);

        var saksnummer = morSak.getSaksnummer();
        opprettNotat(new OpprettNotatDto(
                "et gammelt notat ",
                saksnummer,
                NotatGjelderType.FAGSAK
        ));

        NotatDto morNotat = hentForFagsak(saksnummer).stream().findFirst().orElseThrow();
        long førsteVersjon = morNotat.versjon();

        endreNotat(new EndreNotatDto(
                morNotat.notatId(),
                "et endret notat",
                saksnummer,
                førsteVersjon));

        NotatDto endretNotat = hentNotat(saksnummer, morNotat.notatId());
        assertThat(endretNotat.versjon()).isEqualTo(1);

        assertThatThrownBy(() ->
                endreNotat(new EndreNotatDto(
                        morNotat.notatId(),
                        "endrer gammel versjon",
                        saksnummer,
                        førsteVersjon))
        ).isInstanceOf(TekniskException.class);
    }


    private NotatDto hentNotat(Saksnummer saksnummer, UUID uuid) {
        @SuppressWarnings("unchecked")
        var entity = (Collection<NotatDto>) notatRestTjeneste.hent(new SaksnummerDto(saksnummer), uuid)
                .getEntity();
        return entity.stream().findFirst().orElseThrow();
    }

    private void skjulNotat(SkjulNotatDto skjulNotatDto) {
        notatRestTjeneste.skjul(skjulNotatDto);
    }

    private void opprettNotat(OpprettNotatDto notatDto) {
        notatRestTjeneste.opprett(notatDto);
    }

    private List<NotatDto> hentForFagsak(Saksnummer saksnummer) {
        @SuppressWarnings("unchecked")
        var entity = (List<NotatDto>) notatRestTjeneste.hent(new SaksnummerDto(saksnummer), null)
                .getEntity();
        return entity;
    }

    private void endreNotat(EndreNotatDto endreNotatDto) {
        notatRestTjeneste.endre(endreNotatDto);
    }


}
