package no.nav.k9.sak.web.app.tjenester.notat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.notat.NotatGjelderType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.notat.NotatRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.kontrakt.notat.EndreNotatDto;
import no.nav.k9.sak.kontrakt.notat.NotatDto;
import no.nav.k9.sak.kontrakt.notat.OpprettNotatDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

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
    }

    @Test
    void skalOppretteOgHenteNotat() {
        Fagsak fagsak = TestScenarioBuilder.builderMedSøknad().lagreFagsak(repositoryProvider);
        var saksnummer = fagsak.getSaksnummer();
        String tekst = "en tekst med litt notater";

        var notatDto = new OpprettNotatDto(
            tekst,
            saksnummer,
            NotatGjelderType.FAGSAK
        );

        notatRestTjeneste.opprett(notatDto);
        NotatDto notat = hentForFagsak(saksnummer).stream().findFirst().orElseThrow();

        assertThat(notat.getNotatTekst()).isEqualTo(tekst);
        assertThat(notat.isSkjult()).isFalse();
        assertThat(notat.getOpprettetAv()).isEqualTo("VL");
        assertThat(notat.getOpprettetTidspunkt()).isNotNull();
        assertThat(notat.getEndretAv()).isNull();
        assertThat(notat.getEndretTidspunkt()).isNull();

    }

    private List<NotatDto> hentForFagsak(Saksnummer saksnummer) {
        return (List<NotatDto>) notatRestTjeneste.hentForFagsak(saksnummer).getEntity();
    }

    @Test
    void skalSkjuleNotat() {
        var saksnummer = TestScenarioBuilder.builderMedSøknad().lagreFagsak(repositoryProvider).getSaksnummer();
        var notatDto = new OpprettNotatDto(
            "tekst",
            saksnummer,
            NotatGjelderType.FAGSAK
        );

        notatRestTjeneste.opprett(notatDto);
        NotatDto notat = hentForFagsak(saksnummer).stream().findFirst().orElseThrow();
        assertThat(notat.isSkjult()).isFalse();
        notatRestTjeneste.skjul(notat.getUuid(), true);
        NotatDto skjultNotat = notatRestTjeneste.hent(notat.getUuid());
        assertThat(skjultNotat.getUuid()).isEqualTo(notat.getUuid());
        assertThat(skjultNotat.isSkjult()).isTrue();
        assertThat(skjultNotat.getOpprettetTidspunkt()).isEqualTo(notat.getOpprettetTidspunkt());
        assertThat(skjultNotat.getEndretTidspunkt()).isNotNull();
    }

    @Test
    void skalEndreTekst() {
        var mor = AktørId.dummy();
        var pleietrengende = AktørId.dummy();

        var morSak = TestScenarioBuilder.builderMedSøknad(mor).medPleietrengende(pleietrengende).lagreFagsak(repositoryProvider);

        String morTekst = "et gammelt notat ";
        var saksnummer = morSak.getSaksnummer();
        notatRestTjeneste.opprett(new OpprettNotatDto(
            morTekst,
            saksnummer,
            NotatGjelderType.FAGSAK
        ));

        List<NotatDto> morNotater = hentForFagsak(saksnummer);
        NotatDto morNotat = morNotater.stream().findFirst().orElseThrow();
        assertThat(morNotat.getGjelderType()).isEqualTo(NotatGjelderType.FAGSAK);
        assertThat(morNotat.getNotatTekst()).isEqualTo(morTekst);
        assertThat(morNotat.getOpprettetTidspunkt()).isNotNull();
        assertThat(morNotat.getEndretTidspunkt()).isNull();

        var endretTekst = "et endret notat";
        notatRestTjeneste.endre(morNotat.getUuid(), new EndreNotatDto(
            morNotat.getUuid(),
            endretTekst,
            morNotat.getVersjon()));

        NotatDto endretNotat = notatRestTjeneste.hent(morNotat.getUuid());
        assertThat(endretNotat.getGjelderType()).isEqualTo(NotatGjelderType.FAGSAK);
        assertThat(endretNotat.getNotatTekst()).isEqualTo(endretTekst);
        assertThat(endretNotat.getOpprettetAv()).isEqualTo(morNotat.getOpprettetAv());
        assertThat(endretNotat.getEndretTidspunkt()).isAfter(morNotat.getOpprettetTidspunkt());



    }

    @Test
    void skalOppretteNotatPåPleietrengendeOgHentePåAnnenForeldresSak() {
        var mor = AktørId.dummy();
        var far = AktørId.dummy();
        var pleietrengende = AktørId.dummy();

        var morSak = TestScenarioBuilder.builderMedSøknad(mor).medPleietrengende(pleietrengende).lagreFagsak(repositoryProvider).getSaksnummer();
        var farSak = TestScenarioBuilder.builderUtenSøknad(far).medPleietrengende(pleietrengende).lagreFagsak(repositoryProvider).getSaksnummer();

        String morNotat = "notat som gjelder mor";
        notatRestTjeneste.opprett(new OpprettNotatDto(
            morNotat,
            morSak,
            NotatGjelderType.FAGSAK
        ));
        String pleietrengedeNotat = "notat som gjelder pleietrengende";
        notatRestTjeneste.opprett(new OpprettNotatDto(
            pleietrengedeNotat,
            morSak,
            NotatGjelderType.PLEIETRENGENDE
        ));

        String farNotat = "notat som gjelder far";
        notatRestTjeneste.opprett(new OpprettNotatDto(
            farNotat,
            farSak,
            NotatGjelderType.FAGSAK
        ));

        List<NotatDto> morNotater = hentForFagsak(morSak);
        assertThat(morNotater).hasSize(2);
        assertThat(morNotater).extracting(NotatDto::getNotatTekst).containsExactlyInAnyOrder(morNotat, pleietrengedeNotat);


        List<NotatDto> farNotater = hentForFagsak(farSak);
        assertThat(farNotater).hasSize(2);
        assertThat(farNotater).extracting(NotatDto::getNotatTekst).containsExactlyInAnyOrder(farNotat, pleietrengedeNotat);

    }




    @Test
    void notatPåPsbPleietrengendeSkalIkkeHentesPåPils() {
        //TODO
    }

}
