package no.nav.ung.ytelse.aktivitetspenger.medlemskap;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.geografisk.Landkoder;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapPeriode;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittForutgåendeMedlemskapRepository;
import no.nav.ung.sak.behandlingslager.behandling.medlemskap.OppgittUtenlandsopphold;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder.MottattDokumentTestGrunnlag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class ForutgåendeMedlemskapTjenesteTest {

    private static final JournalpostId JP = new JournalpostId("JP1");

    @Inject
    private EntityManager entityManager;

    @Inject
    private OppgittForutgåendeMedlemskapRepository forutgåendeMedlemskapRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private VilkårResultatRepository vilkårResultatRepository;
    @Inject
    private StartdatoRepository startdatoRepository;
    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;

    @Inject
    @Any
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;


    @Test
    void skal_mappe_oppgitt_periode_til_medlemskap_dto() {
        var tjeneste = new ForutgåendeMedlemskapTjeneste(forutgåendeMedlemskapRepository, behandlingRepository, vilkårResultatRepository, startdatoRepository, perioderTilVurderingTjenester);

        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medMottattDokument(new MottattDokumentTestGrunnlag(null, null, LocalDateTime.now(), JP))
            .lagre(entityManager);

        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(),
            OppgittForutgåendeMedlemskapPeriode.builder()
                .medJournalpostId(JP)
                .medFom(LocalDate.of(2019, 7, 1))
                .medTom(LocalDate.of(2024, 6, 30))
                .medHarBoddINorge(false)
                .medHarJobbetINorge(false)
                .medHarJobbetUtenforNorge(true)
                .medUtenlandsopphold(Set.of(
                    new OppgittUtenlandsopphold(LocalDate.of(2019, 7, 1), LocalDate.of(2022, 12, 31), Landkoder.SWE, true, "010185-1234"),
                    new OppgittUtenlandsopphold(LocalDate.of(2023, 1, 1), LocalDate.of(2024, 6, 30), Landkoder.fraKode("DEU"), false, null)
                ))
                .build());

        var medlemskapDto = tjeneste.hentMedlemskapForBehandlingSomDto(behandling.getId()).stream().findFirst().orElseThrow();

        assertThat(medlemskapDto.journalpostId()).isEqualTo(JP.getVerdi());
        assertThat(medlemskapDto.harBoddINorge()).isFalse();
        assertThat(medlemskapDto.harJobbetINorge()).isFalse();
        assertThat(medlemskapDto.harJobbetUtenforNorge()).isTrue();
        assertThat(medlemskapDto.utenlandsopphold()).hasSize(2);

        var sverige = medlemskapDto.utenlandsopphold().stream()
            .filter(u -> u.landkode().equals("SWE"))
            .findFirst().orElseThrow();
        assertThat(sverige.land()).isEqualTo("Sverige");
        assertThat(sverige.harTrygdeavtale()).isTrue();
        assertThat(sverige.periode().getFom()).isEqualTo(LocalDate.of(2019, 7, 1));
        assertThat(sverige.periode().getTom()).isEqualTo(LocalDate.of(2022, 12, 31));
        assertThat(sverige.harJobbetIPerioden()).isTrue();
        assertThat(sverige.utenlandskNasjonalId()).isEqualTo("010185-1234");

        var tyskland = medlemskapDto.utenlandsopphold().stream()
            .filter(u -> u.landkode().equals("DEU"))
            .findFirst().orElseThrow();
        assertThat(tyskland.land()).isEqualTo("Tyskland");
        assertThat(tyskland.harJobbetIPerioden()).isFalse();
        assertThat(tyskland.utenlandskNasjonalId()).isNull();
    }

    @Test
    void skal_koble_riktig_medlemskap_til_riktig_vilkårsperiode_basert_på_startdato_og_journalpost() {
        var tjeneste = new ForutgåendeMedlemskapTjeneste(forutgåendeMedlemskapRepository, behandlingRepository, vilkårResultatRepository, startdatoRepository, perioderTilVurderingTjenester);

        var periode1 = new Periode(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 31));
        var periode2 = new Periode(LocalDate.of(2025, 8, 1), LocalDate.of(2025, 9, 30));
        var ikkeRelevantPeriode = new Periode(LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 31));
        var jp1 = new JournalpostId("JP1");
        var jp2 = new JournalpostId("JP2");

        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.OPPFYLT, periode1)
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_OPPFYLT, periode2, Avslagsårsak.SØKER_ER_IKKE_MEDLEM, null)
            .leggTilVilkår(VilkårType.FORUTGÅENDE_MEDLEMSKAPSVILKÅRET, Utfall.IKKE_RELEVANT, ikkeRelevantPeriode)
            .medMottattDokument(new MottattDokumentTestGrunnlag(null, null, LocalDateTime.now(), jp1))
            .lagre(entityManager);

        // To oppgitte medlemskapsperioder, koblet til hver sin journalpost
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(),
            OppgittForutgåendeMedlemskapPeriode.builder()
                .medJournalpostId(jp1)
                .medFom(periode1.getFom().minusYears(5))
                .medTom(periode1.getFom().minusDays(1))
                .medHarBoddINorge(true)
                .medHarJobbetINorge(true)
                .medHarJobbetUtenforNorge(false)
                .medUtenlandsopphold(Set.of())
                .build());
        forutgåendeMedlemskapRepository.leggTilOppgittPeriode(behandling.getId(),
            OppgittForutgåendeMedlemskapPeriode.builder()
                .medJournalpostId(jp2)
                .medFom(periode2.getFom().minusYears(5))
                .medTom(periode2.getFom().minusDays(1))
                .medHarBoddINorge(false)
                .medHarJobbetINorge(false)
                .medHarJobbetUtenforNorge(true)
                .medUtenlandsopphold(Set.of(
                    new OppgittUtenlandsopphold(periode2.getFom().minusYears(2), periode2.getFom().minusDays(1), Landkoder.SWE, true, null)
                ))
                .build());

        // Startdatoer knytter journalpost til hvilken vilkårsperiode den gjelder for
        startdatoRepository.lagre(behandling.getId(), List.of(
            new SøktStartdato(periode1.getFom(), jp1),
            new SøktStartdato(periode2.getFom(), jp2)
        ));
        // "Relevante" startdatoer avgjør hvilke søknadsperioder som ligger til grunn for prosesstriggere/til-vurdering-tidslinjen
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(List.of(new SøktStartdato(periode1.getFom(), jp1))));

        // Kun periode1 er markert som til vurdering i denne behandlingen
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fraOgMedTilOgMed(periode1.getFom(), periode1.getTom()))));

        var response = tjeneste.hentMedlemskapOgVilkårSomDto(new BehandlingUuidDto(behandling.getUuid()));

        // IKKE_RELEVANT-perioden skal være filtrert bort
        assertThat(response.perioder())
            .extracting(it -> new Periode(it.periode().getFom(), it.periode().getTom()))
            .containsExactlyInAnyOrder(periode1, periode2);

        var infoForPeriode1 = response.perioder().stream()
            .filter(it -> it.periode().getFom().equals(periode1.getFom()))
            .findFirst().orElseThrow();
        assertThat(infoForPeriode1.medlemskapFraBruker()).isNotNull();
        assertThat(infoForPeriode1.medlemskapFraBruker().journalpostId()).isEqualTo(jp1.getVerdi());
        assertThat(infoForPeriode1.medlemskapFraBruker().harBoddINorge()).isTrue();
        assertThat(infoForPeriode1.medlemskapFraBruker().utenlandsopphold()).isEmpty();
        assertThat(infoForPeriode1.vurderesIBehandlingen()).isTrue();

        var infoForPeriode2 = response.perioder().stream()
            .filter(it -> it.periode().getFom().equals(periode2.getFom()))
            .findFirst().orElseThrow();
        assertThat(infoForPeriode2.medlemskapFraBruker()).isNotNull();
        assertThat(infoForPeriode2.medlemskapFraBruker().journalpostId()).isEqualTo(jp2.getVerdi());
        assertThat(infoForPeriode2.medlemskapFraBruker().harBoddINorge()).isFalse();
        assertThat(infoForPeriode2.medlemskapFraBruker().utenlandsopphold()).hasSize(1);
        assertThat(infoForPeriode2.vurderesIBehandlingen()).isFalse();
        assertThat(infoForPeriode2.avslagsårsak()).isEqualTo(MedlemskapAvslagsÅrsakType.SØKER_IKKE_MEDLEM);
    }
}
