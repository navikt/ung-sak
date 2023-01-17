package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.visning.institusjon;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.Response;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon.GodkjentOpplæringsinstitusjonTjeneste;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjonPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjonHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderHolder;

@CdiDbAwareTest
class InstitusjonRestTjenesteTest {

    private InstitusjonRestTjeneste restTjeneste;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private VurdertOpplæringRepository vurdertOpplæringRepository;
    @Inject
    private GodkjentOpplæringsinstitusjonTjeneste godkjentOpplæringsinstitusjonTjeneste;
    @Inject
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    @Inject
    private EntityManager entityManager;

    private Behandling behandling;
    private final JournalpostId journalpostId1 = new JournalpostId("1776");
    private final JournalpostId journalpostId2 = new JournalpostId("1789");
    private final UUID institusjonUuid = UUID.randomUUID();
    private final String institusjonNavn1 = "Livets skole";
    private final String institusjonNavn2 = "Fyrstikkaleen barnehage";
    private final Periode kursperiode1 = new Periode(LocalDate.now().minusWeeks(1), LocalDate.now());
    private final Periode kursperiode2 = new Periode(LocalDate.now().plusDays(1), LocalDate.now().plusWeeks(1));

    @BeforeEach
    void setup() {
        restTjeneste = new InstitusjonRestTjeneste(behandlingRepository, vurdertOpplæringRepository, godkjentOpplæringsinstitusjonTjeneste, uttakPerioderGrunnlagRepository);

        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OPPLÆRINGSPENGER);
        behandling = scenario.lagre(repositoryProvider);
    }

    private PerioderFraSøknad lagPerioderFraSøknad(JournalpostId journalpostId, Periode kursperiode, String institusjonNavn, UUID institusjonUuid) {
        PerioderFraSøknad perioderFraSøknad = new PerioderFraSøknad(journalpostId,
            List.of(new UttakPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(kursperiode.getFom(), kursperiode.getTom()), Duration.ofHours(7).plusMinutes(30))),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            List.of(new KursPeriode(kursperiode.getFom(), kursperiode.getTom(), null, null, institusjonNavn, institusjonUuid, null, null)));

        uttakPerioderGrunnlagRepository.lagre(behandling.getId(), perioderFraSøknad);
        return perioderFraSøknad;
    }

    @Test
    void ingenVurdering() {
        var perioderFraSøknad = lagPerioderFraSøknad(journalpostId1, kursperiode1, institusjonNavn1, institusjonUuid);
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad)));

        Response response = restTjeneste.hentVurdertInstitusjon(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        InstitusjonerDto result = (InstitusjonerDto) response.getEntity();
        assertThat(result).isNotNull();

        assertThat(result.getPerioder()).hasSize(1);
        assertThat(result.getPerioder().get(0).getPeriode()).isEqualTo(kursperiode1);
        assertThat(result.getPerioder().get(0).getInstitusjon()).isEqualTo(institusjonNavn1);
        assertThat(result.getPerioder().get(0).getJournalpostId().getJournalpostId()).isEqualTo(journalpostId1);

        assertThat(result.getVurderinger()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getPerioder()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getPerioder().get(0)).isEqualTo(kursperiode1);
        assertThat(result.getVurderinger().get(0).getResultat()).isEqualTo(Resultat.MÅ_VURDERES);
        assertThat(result.getVurderinger().get(0).getJournalpostId().getJournalpostId()).isEqualTo(journalpostId1);
        assertThat(result.getVurderinger().get(0).getBegrunnelse()).isNull();
        assertThat(result.getVurderinger().get(0).getEndretAv()).isNull();
        assertThat(result.getVurderinger().get(0).getEndretTidspunkt()).isNull();
    }

    @Test
    void automatiskVurdering() {
        var perioderFraSøknad = lagPerioderFraSøknad(journalpostId1, kursperiode1, institusjonNavn1, institusjonUuid);
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad)));

        var godkjentPeriode = new GodkjentOpplæringsinstitusjonPeriode(kursperiode1.getFom(), kursperiode1.getTom());
        var godkjentInstitusjon = new GodkjentOpplæringsinstitusjon(institusjonUuid, institusjonNavn1, List.of(godkjentPeriode));
        entityManager.persist(godkjentInstitusjon);
        entityManager.flush();

        Response response = restTjeneste.hentVurdertInstitusjon(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        InstitusjonerDto result = (InstitusjonerDto) response.getEntity();
        assertThat(result).isNotNull();

        assertThat(result.getPerioder()).hasSize(1);
        assertThat(result.getPerioder().get(0).getPeriode()).isEqualTo(kursperiode1);
        assertThat(result.getPerioder().get(0).getInstitusjon()).isEqualTo(institusjonNavn1);
        assertThat(result.getPerioder().get(0).getJournalpostId().getJournalpostId()).isEqualTo(journalpostId1);

        assertThat(result.getVurderinger()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getPerioder()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getPerioder().get(0)).isEqualTo(kursperiode1);
        assertThat(result.getVurderinger().get(0).getResultat()).isEqualTo(Resultat.GODKJENT_AUTOMATISK);
        assertThat(result.getVurderinger().get(0).getJournalpostId().getJournalpostId()).isEqualTo(journalpostId1);
        assertThat(result.getVurderinger().get(0).getBegrunnelse()).isNull();
        assertThat(result.getVurderinger().get(0).getEndretAv()).isNull();
        assertThat(result.getVurderinger().get(0).getEndretTidspunkt()).isNull();
    }

    @Test
    void manuellVurdering() {
        var perioderFraSøknad = lagPerioderFraSøknad(journalpostId1, kursperiode1, institusjonNavn1, null);
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad)));

        var vurdertInstitusjon = new VurdertInstitusjon(journalpostId1, true, "fordi");
        vurdertOpplæringRepository.lagre(behandling.getId(), new VurdertInstitusjonHolder(List.of(vurdertInstitusjon)));

        Response response = restTjeneste.hentVurdertInstitusjon(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        InstitusjonerDto result = (InstitusjonerDto) response.getEntity();
        assertThat(result).isNotNull();

        assertThat(result.getPerioder()).hasSize(1);
        assertThat(result.getPerioder().get(0).getPeriode()).isEqualTo(kursperiode1);
        assertThat(result.getPerioder().get(0).getInstitusjon()).isEqualTo(institusjonNavn1);
        assertThat(result.getPerioder().get(0).getJournalpostId().getJournalpostId()).isEqualTo(journalpostId1);

        assertThat(result.getVurderinger()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getPerioder()).hasSize(1);
        assertThat(result.getVurderinger().get(0).getPerioder().get(0)).isEqualTo(kursperiode1);
        assertThat(result.getVurderinger().get(0).getResultat()).isEqualTo(Resultat.GODKJENT_MANUELT);
        assertThat(result.getVurderinger().get(0).getJournalpostId().getJournalpostId()).isEqualTo(journalpostId1);
        assertThat(result.getVurderinger().get(0).getBegrunnelse()).isEqualTo("fordi");
        assertThat(result.getVurderinger().get(0).getEndretAv()).isEqualTo("VL");
        assertThat(result.getVurderinger().get(0).getEndretTidspunkt()).isNotNull();
    }

    @Test
    void kombinertVurdering() {
        var perioderFraSøknad1 = lagPerioderFraSøknad(journalpostId1, kursperiode1, institusjonNavn1, institusjonUuid);
        var perioderFraSøknad2 = lagPerioderFraSøknad(journalpostId2, kursperiode2, institusjonNavn2, null);
        uttakPerioderGrunnlagRepository.lagreRelevantePerioder(behandling.getId(), new UttakPerioderHolder(Set.of(perioderFraSøknad1, perioderFraSøknad2)));

        var godkjentPeriode = new GodkjentOpplæringsinstitusjonPeriode(kursperiode1.getFom(), kursperiode1.getTom());
        var godkjentInstitusjon = new GodkjentOpplæringsinstitusjon(institusjonUuid, institusjonNavn1, List.of(godkjentPeriode));
        entityManager.persist(godkjentInstitusjon);
        entityManager.flush();

        var vurdertInstitusjon = new VurdertInstitusjon(journalpostId2, false, "nei");
        vurdertOpplæringRepository.lagre(behandling.getId(), new VurdertInstitusjonHolder(List.of(vurdertInstitusjon)));

        Response response = restTjeneste.hentVurdertInstitusjon(new BehandlingUuidDto(behandling.getUuid()));
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);

        InstitusjonerDto result = (InstitusjonerDto) response.getEntity();
        assertThat(result).isNotNull();
        assertThat(result.getPerioder()).hasSize(2);
        assertThat(result.getVurderinger()).hasSize(2);

        InstitusjonVurderingDto automatiskVurdering = result.getVurderinger().stream().filter(v -> v.getJournalpostId().getJournalpostId().equals(journalpostId1)).findFirst().orElseThrow();
        assertThat(automatiskVurdering.getPerioder()).hasSize(1);
        assertThat(automatiskVurdering.getPerioder().get(0)).isEqualTo(kursperiode1);
        assertThat(automatiskVurdering.getResultat()).isEqualTo(Resultat.GODKJENT_AUTOMATISK);

        InstitusjonVurderingDto manuellVurdering = result.getVurderinger().stream().filter(v -> v.getJournalpostId().getJournalpostId().equals(journalpostId2)).findFirst().orElseThrow();
        assertThat(manuellVurdering.getPerioder()).hasSize(1);
        assertThat(manuellVurdering.getPerioder().get(0)).isEqualTo(kursperiode2);
        assertThat(manuellVurdering.getResultat()).isEqualTo(Resultat.IKKE_GODKJENT_MANUELT);
    }
}
