package no.nav.ung.sak.domene.behandling.steg.kompletthet;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VurderKompletthetStegImplTest {

    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private EtterlysningRepository etterlysningRepository;

    private VurderKompletthetStegImpl vurderKompletthetSteg;

    private Fagsak fagsak;
    private Behandling førstegangsbehandling;
    private Behandling revurdering;
    private AktørId aktørId;

    @BeforeEach
    void setUp() {
        final var fom = LocalDate.now();
        aktørId = AktørId.dummy();
        fagsak = new Fagsak(FagsakYtelseType.UNGDOMSYTELSE, aktørId, new Saksnummer("SAKEN"), fom, fom.plusWeeks(52));
        fagsakRepository.opprettNy(fagsak);
        førstegangsbehandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(førstegangsbehandling, behandlingRepository.taSkriveLås(førstegangsbehandling));

        revurdering = Behandling.fraTidligereBehandling(førstegangsbehandling, BehandlingType.REVURDERING).build();

        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));

        vurderKompletthetSteg = new VurderKompletthetStegImpl(etterlysningRepository, "P14D");

    }

    @Test
    void skal_ikke_returnere_aksjonspunkter_uten_etterlysninger() {

        final var resultat = vurderKompletthetSteg.utførSteg(new BehandlingskontrollKontekst(revurdering.getFagsakId(), aktørId, behandlingRepository.taSkriveLås(revurdering)));

        assertThat(resultat.getAksjonspunktListe().size()).isEqualTo(0);
    }




    @Test
    void skal_returnere_autopunkt_for_inntektuttalelse_som_er_opprettet() {
        // Arrange
        etterlysningRepository.lagre(Etterlysning.opprettForType(revurdering.getId(), UUID.randomUUID(), UUID.randomUUID(), DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()), EtterlysningType.UTTALELSE_KONTROLL_INNTEKT));

        // Act
        final var resultat = vurderKompletthetSteg.utførSteg(new BehandlingskontrollKontekst(revurdering.getFagsakId(), aktørId, behandlingRepository.taSkriveLås(revurdering)));

        // Assert
        assertThat(resultat.getAksjonspunktListe().size()).isEqualTo(1);
        assertThat(resultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE);
        assertThat(resultat.getAksjonspunktResultater().get(0).getFrist()).isNotNull();
    }

    @Test
    void skal_returnere_autopunkt_for_inntektuttalelse_som_satt_på_vent() {
        // Arrange
        final var etterlysning = Etterlysning.opprettForType(revurdering.getId(), UUID.randomUUID(), UUID.randomUUID(), DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()), EtterlysningType.UTTALELSE_KONTROLL_INNTEKT);
        final var frist = LocalDateTime.now().plusDays(1);
        etterlysning.vent(frist);
        etterlysningRepository.lagre(etterlysning);

        // Act
        final var resultat = vurderKompletthetSteg.utførSteg(new BehandlingskontrollKontekst(revurdering.getFagsakId(), aktørId, behandlingRepository.taSkriveLås(revurdering)));

        // Assert
        assertThat(resultat.getAksjonspunktListe().size()).isEqualTo(1);
        assertThat(resultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE);
        assertThat(resultat.getAksjonspunktResultater().get(0).getFrist()).isEqualTo(frist);
    }

    @Test
    void skal_returnere_autopunkt_for_inntektuttalelse_som_satt_på_vent_og_opprettet() {
        // Arrange
        final var etterlysning1 = Etterlysning.opprettForType(revurdering.getId(), UUID.randomUUID(), UUID.randomUUID(), DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()), EtterlysningType.UTTALELSE_KONTROLL_INNTEKT);
        final var frist = LocalDateTime.now().plusDays(1);
        etterlysning1.vent(frist);
        etterlysningRepository.lagre(etterlysning1);

        final var etterlysning2 = Etterlysning.opprettForType(revurdering.getId(), UUID.randomUUID(), UUID.randomUUID(), DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()), EtterlysningType.UTTALELSE_KONTROLL_INNTEKT);
        etterlysningRepository.lagre(etterlysning2);

        // Act
        final var resultat = vurderKompletthetSteg.utførSteg(new BehandlingskontrollKontekst(revurdering.getFagsakId(), aktørId, behandlingRepository.taSkriveLås(revurdering)));

        // Assert
        assertThat(resultat.getAksjonspunktListe().size()).isEqualTo(1);
        assertThat(resultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE);
        assertThat(resultat.getAksjonspunktResultater().get(0).getFrist()).isNotEqualTo(frist);
    }

    @Test
    void skal_returnere_autopunkt_for_inntektuttalelse_og_bekreftelse_av_ungdomsprogram() {
        // Arrange
        final var etterlysning1 = Etterlysning.opprettForType(revurdering.getId(), UUID.randomUUID(), UUID.randomUUID(), DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()), EtterlysningType.UTTALELSE_KONTROLL_INNTEKT);
        etterlysningRepository.lagre(etterlysning1);

        final var etterlysning2 = Etterlysning.opprettForType(revurdering.getId(), UUID.randomUUID(), UUID.randomUUID(), DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()), EtterlysningType.UTTALELSE_ENDRET_PROGRAMPERIODE);
        etterlysningRepository.lagre(etterlysning2);

        // Act
        final var resultat = vurderKompletthetSteg.utførSteg(new BehandlingskontrollKontekst(revurdering.getFagsakId(), aktørId, behandlingRepository.taSkriveLås(revurdering)));

        // Assert
        assertThat(resultat.getAksjonspunktListe().size()).isEqualTo(2);
        assertThat(resultat.getAksjonspunktListe().stream().anyMatch(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE::equals)).isTrue();
        assertThat(resultat.getAksjonspunktListe().stream().anyMatch(AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING::equals)).isTrue();
    }

}
