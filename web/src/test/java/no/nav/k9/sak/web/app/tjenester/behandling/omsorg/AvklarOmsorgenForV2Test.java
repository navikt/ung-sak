package no.nav.k9.sak.web.app.tjenester.behandling.omsorg;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentStatus;
import no.nav.k9.kodeverk.sykdom.Resultat;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.db.util.CdiDbAwareTest;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.omsorg.repo.OmsorgenFor;
import no.nav.k9.sak.inngangsvilkår.omsorg.repo.OmsorgenForGrunnlag;
import no.nav.k9.sak.inngangsvilkår.omsorg.repo.OmsorgenForGrunnlagRepository;
import no.nav.k9.sak.inngangsvilkår.omsorg.repo.OmsorgenForPeriode;
import no.nav.k9.sak.kontrakt.omsorg.AvklarOmsorgenForDto;
import no.nav.k9.sak.kontrakt.omsorg.OmsorgenForOppdateringDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperiodeRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperioder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.SøknadsperioderHolder;

@CdiDbAwareTest
class AvklarOmsorgenForV2Test {

    @Inject
    @Any
    private AvklarOmsorgenForV2 avklarOmsorgenForV2;
    @Inject
    private SøknadsperiodeRepository søknadsperiodeRepository;
    @Inject
    private MottatteDokumentRepository mottatteDokumentRepository;
    @Inject
    private OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository;
    @Inject
    private EntityManager entityManager;
    private Behandling behandling;
    private final Periode søknadsperiode = new Periode(LocalDate.now().minusWeeks(2), LocalDate.now());

    @BeforeEach
    void setup() {
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad().medSøknadDato(LocalDate.now());
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_OMSORGEN_FOR_V2, BehandlingStegType.VURDER_OMSORG_FOR);
        behandling = scenario.lagre(entityManager);

        JournalpostId journalpostId = new JournalpostId("1");
        søknadsperiodeRepository.lagre(behandling.getId(), new Søknadsperioder(journalpostId, new Søknadsperiode(DatoIntervallEntitet.fra(søknadsperiode), false)));
        søknadsperiodeRepository.lagreRelevanteSøknadsperioder(behandling.getId(), new SøknadsperioderHolder(new Søknadsperioder(journalpostId, new Søknadsperiode(DatoIntervallEntitet.fra(søknadsperiode), false))));
        mottatteDokumentRepository.lagre(new MottattDokument.Builder().medType(Brevkode.PLEIEPENGER_BARN_SOKNAD).medInnsendingstidspunkt(LocalDateTime.now().minusMinutes(1)).medJournalPostId(journalpostId).medFagsakId(behandling.getFagsakId()).build(), DokumentStatus.GYLDIG);
    }

    @Test
    void skalLagreNyttGrunnlagOgArveFraEksisterende() {
        Periode periode1 = new Periode(søknadsperiode.getFom(), søknadsperiode.getFom().plusWeeks(1));
        Periode periode2 = new Periode(søknadsperiode.getFom().plusWeeks(1).plusDays(1), søknadsperiode.getTom());

        setBruker("enball");
        AvklarOmsorgenForDto dto1 = new AvklarOmsorgenForDto("", List.of(new OmsorgenForOppdateringDto(periode1, "fordi", Resultat.OPPFYLT)), List.of());
        oppdater(dto1);

        setBruker("toball");
        AvklarOmsorgenForDto dto2 = new AvklarOmsorgenForDto("", List.of(new OmsorgenForOppdateringDto(periode2, "derfor", Resultat.OPPFYLT)), List.of());
        oppdater(dto2);

        OmsorgenForGrunnlag grunnlag = omsorgenForGrunnlagRepository.hent(behandling.getId());
        OmsorgenFor omsorgenFor = grunnlag.getOmsorgenFor();
        assertThat(omsorgenFor).isNotNull();
        assertThat(omsorgenFor.getPerioder()).hasSize(2);
        OmsorgenForPeriode omsorgenForPeriode1 = omsorgenFor.getPerioder().stream().filter(omsorgenForPeriode -> omsorgenForPeriode.getPeriode().tilPeriode().equals(periode1)).findFirst().orElseThrow();
        assertThat(omsorgenForPeriode1.getBegrunnelse()).isEqualTo("fordi");
        assertThat(omsorgenForPeriode1.getVurdertAv()).isEqualTo("enball");
        assertThat(omsorgenForPeriode1.getResultat()).isEqualTo(Resultat.OPPFYLT);
        OmsorgenForPeriode omsorgenForPeriode2 = omsorgenFor.getPerioder().stream().filter(omsorgenForPeriode -> omsorgenForPeriode.getPeriode().tilPeriode().equals(periode2)).findFirst().orElseThrow();
        assertThat(omsorgenForPeriode2.getBegrunnelse()).isEqualTo("derfor");
        assertThat(omsorgenForPeriode2.getVurdertAv()).isEqualTo("toball");
        assertThat(omsorgenForPeriode2.getResultat()).isEqualTo(Resultat.OPPFYLT);
        assertThat(omsorgenForPeriode2.getVurdertTidspunkt()).isAfter(omsorgenForPeriode1.getVurdertTidspunkt());
    }

    private void oppdater(AvklarOmsorgenForDto dto) {
        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        OppdateringResultat resultat = avklarOmsorgenForV2.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        assertThat(resultat).isNotNull();
    }

    private static void setBruker(String brukerId) {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker(brukerId);
    }
}
