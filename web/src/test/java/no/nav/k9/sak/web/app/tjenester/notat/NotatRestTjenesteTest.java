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
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.notat.NotatRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.kontrakt.notat.EndreNotatDto;
import no.nav.k9.sak.kontrakt.notat.NotatDto;
import no.nav.k9.sak.kontrakt.notat.NyttNotatDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;

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
        Long fagsakId = TestScenarioBuilder.builderMedSøknad().lagreFagsak(repositoryProvider).getId();
        String tekst = "en tekst med litt notater";

        var notatDto = new NyttNotatDto(
            tekst,
            fagsakId,
            NotatGjelderType.FAGSAK
        );

        notatRestTjeneste.opprett(notatDto);
        NotatDto notat = notatRestTjeneste.hentForFagsak(fagsakId).stream().findFirst().orElseThrow();

        assertThat(notat.getNotatTekst()).isEqualTo(tekst);
        assertThat(notat.isSkjult()).isFalse();
        assertThat(notat.getOpprettetAv()).isEqualTo("VL");
        assertThat(notat.getOpprettetTidspunkt()).isNotNull();
        assertThat(notat.getEndretAv()).isNull();
        assertThat(notat.getEndretTidspunkt()).isNull();

    }

    @Test
    void skalSkjuleNotat() {
        Long fagsakId = TestScenarioBuilder.builderMedSøknad().lagreFagsak(repositoryProvider).getId();
        var notatDto = new NyttNotatDto(
            "tekst",
            fagsakId,
            NotatGjelderType.FAGSAK
        );

        notatRestTjeneste.opprett(notatDto);
        NotatDto notat = notatRestTjeneste.hentForFagsak(fagsakId).stream().findFirst().orElseThrow();
        assertThat(notat.isSkjult()).isFalse();
        notatRestTjeneste.skjul(notat.getUuid(), true);
        NotatDto skjultNotat = notatRestTjeneste.hent(notat.getUuid());
        assertThat(skjultNotat.isSkjult()).isTrue();
        assertThat(skjultNotat.getOpprettetTidspunkt()).isEqualTo(notat.getOpprettetTidspunkt());
        assertThat(skjultNotat.getEndretTidspunkt()).isNotNull();
    }

    @Test
    void skalEndreTekst() {
        var mor = AktørId.dummy();
        var pleietrengende = AktørId.dummy();

        var morSak = TestScenarioBuilder.builderMedSøknad(mor).medPleietrengende(pleietrengende).lagreFagsak(repositoryProvider);

        String morTekst = "et nytt notat ";
        notatRestTjeneste.opprett(new NyttNotatDto(
            morTekst,
            morSak.getId(),
            NotatGjelderType.FAGSAK
        ));

        List<NotatDto> morNotater = notatRestTjeneste.hentForFagsak(morSak.getId());
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

        var morSak = TestScenarioBuilder.builderMedSøknad(mor).medPleietrengende(pleietrengende).lagreFagsak(repositoryProvider);
        var farSak = TestScenarioBuilder.builderUtenSøknad(far).medPleietrengende(pleietrengende).lagreFagsak(repositoryProvider);

        String morNotat = "notat som gjelder mor";
        notatRestTjeneste.opprett(new NyttNotatDto(
            morNotat,
            morSak.getId(),
            NotatGjelderType.FAGSAK
        ));
        String pleietrengedeNotat = "notat som gjelder pleietrengende";
        notatRestTjeneste.opprett(new NyttNotatDto(
            pleietrengedeNotat,
            morSak.getId(),
            NotatGjelderType.PLEIETRENGENDE
        ));

        String farNotat = "notat som gjelder far";
        notatRestTjeneste.opprett(new NyttNotatDto(
            farNotat,
            farSak.getId(),
            NotatGjelderType.FAGSAK
        ));

        List<NotatDto> morNotater = notatRestTjeneste.hentForFagsak(morSak.getId());
        assertThat(morNotater).hasSize(2);
        assertThat(morNotater).extracting(NotatDto::getNotatTekst).containsExactlyInAnyOrder(morNotat, pleietrengedeNotat);


        List<NotatDto> farNotater = notatRestTjeneste.hentForFagsak(farSak.getId());
        assertThat(farNotater).hasSize(2);
        assertThat(farNotater).extracting(NotatDto::getNotatTekst).containsExactlyInAnyOrder(farNotat, pleietrengedeNotat);

    }




    @Test
    void notatPåPsbPleietrengendeSkalIkkeHentesPåPils() {
        //TODO
    }

}
