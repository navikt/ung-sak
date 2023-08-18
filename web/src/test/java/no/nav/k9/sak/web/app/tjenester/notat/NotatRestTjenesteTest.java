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

        assertThat(notat.notatTekst()).isEqualTo(tekst);
        assertThat(notat.fagsakId()).isEqualTo(fagsakId);
        assertThat(notat.notatGjelderType()).isEqualTo(NotatGjelderType.FAGSAK);
        assertThat(notat.skjult()).isFalse();
        assertThat(notat.opprettetAv()).isEqualTo("VL");
        assertThat(notat.opprettetTidspunkt()).isNotNull();
        assertThat(notat.endretAv()).isNull();
        assertThat(notat.endretTidspunkt()).isNull();

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
        assertThat(notat.skjult()).isFalse();
        notatRestTjeneste.skjul(notat.id(), true);
        NotatDto skjultNotat = notatRestTjeneste.hent(notat.id());
        assertThat(skjultNotat.skjult()).isTrue();
        assertThat(skjultNotat.opprettetTidspunkt()).isNotNull();
        assertThat(skjultNotat.endretTidspunkt()).isNull();
    }

    @Test
    void skalEndreTekstOgPleietrengende() {
        var mor = AktørId.dummy();
        var pleietrengende = AktørId.dummy();

        var morSak = TestScenarioBuilder.builderMedSøknad(mor).medPleietrengende(pleietrengende).lagreFagsak(repositoryProvider);

        String morTekst = "notat som gjelder pleietrengende";
        notatRestTjeneste.opprett(new NyttNotatDto(
            morTekst,
            morSak.getId(),
            NotatGjelderType.FAGSAK
        ));

        List<NotatDto> morNotater = notatRestTjeneste.hentForFagsak(morSak.getId());
        NotatDto morNotat = morNotater.stream().findFirst().orElseThrow();
        assertThat(morNotat.notatGjelderType()).isEqualTo(NotatGjelderType.FAGSAK);
        assertThat(morNotat.notatTekst()).isEqualTo(morTekst);
        assertThat(morNotat.opprettetTidspunkt()).isNotNull();
        assertThat(morNotat.endretTidspunkt()).isNull();

        var pleietrengendeTekst = "notat endret til å gjelde pleietrengende";
        notatRestTjeneste.endre(morNotat.id(), new EndreNotatDto(
            morNotat.id(),
            pleietrengendeTekst,
            NotatGjelderType.PLEIETRENGENDE,
            morNotat.versjon()));

        NotatDto endretNotat = notatRestTjeneste.hent(morNotat.id());
        assertThat(endretNotat.notatGjelderType()).isEqualTo(NotatGjelderType.PLEIETRENGENDE);
        assertThat(endretNotat.notatTekst()).isEqualTo(pleietrengendeTekst);
        assertThat(endretNotat.opprettetTidspunkt()).isEqualTo(morNotat.opprettetTidspunkt());
        assertThat(endretNotat.endretTidspunkt()).isAfter(morNotat.opprettetTidspunkt());



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
        assertThat(morNotater).extracting(NotatDto::notatTekst).containsExactlyInAnyOrder(morNotat, pleietrengedeNotat);


        List<NotatDto> farNotater = notatRestTjeneste.hentForFagsak(farSak.getId());
        assertThat(farNotater).hasSize(2);
        assertThat(farNotater).extracting(NotatDto::notatTekst).containsExactlyInAnyOrder(farNotat, pleietrengedeNotat);

    }

    @Test
    void notatPåPsbPleietrengendeSkalIkkeHentesPåPils() {
        //TODO
    }

}
