package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;

class KjøreplanUtlederTest {

    private final KjøreplanUtleder utleder = new KjøreplanUtleder(null, null, null, null, null, null);

    @Test
    void skal_utlede_rekkefølge_blantKrav() {
        var idag = LocalDate.now();
        var behandling1SøktePerioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(50), idag.minusDays(20)));
        var behandling2SøktePerioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(35), idag.minusDays(10)));
        var mottattDokumenter = List.of(new MottattKrav(new JournalpostId("1"), 100L), new MottattKrav(new JournalpostId("2"), 101L));
        var saksnummer = new Saksnummer("1");
        var førsteKrav = LocalDateTime.now().minusDays(3);
        var behandlingStatus = Map.of(100L, new BehandlingMedMetadata(BehandlingStatus.AVSLUTTET, null), 101L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, null));
        var sakOgBehandling1 = new SakOgBehandlinger(1L, saksnummer, 100L,
            behandlingStatus, mottattDokumenter,
            Map.of(new KravDokument(new JournalpostId("1"), førsteKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling1SøktePerioder)));
        var andreKrav = LocalDateTime.now();
        var sakOgBehandling2 = new SakOgBehandlinger(1L, saksnummer, 101L,
            behandlingStatus, mottattDokumenter,
            Map.of(new KravDokument(new JournalpostId("2"), andreKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling2SøktePerioder)));

        var kravRekkefølge = utleder.utledInternKravprio(List.of(sakOgBehandling1, sakOgBehandling2));

        assertThat(kravRekkefølge).hasSize(3);
        assertThat(kravRekkefølge.getSegment(new LocalDateInterval(idag.minusDays(50), idag.minusDays(36))).getValue())
            .containsExactly(new InternalKravprioritet(1L, saksnummer, new JournalpostId("1"), 100L, BehandlingStatus.AVSLUTTET, førsteKrav));

        assertThat(kravRekkefølge.getSegment(new LocalDateInterval(idag.minusDays(35), idag.minusDays(20)))
            .getValue()).containsExactly(new InternalKravprioritet(1L, saksnummer, new JournalpostId("1"), 100L, BehandlingStatus.AVSLUTTET, førsteKrav), new InternalKravprioritet(1L, saksnummer, new JournalpostId("2"), 101L, BehandlingStatus.UTREDES, andreKrav));

        assertThat(kravRekkefølge.getSegment(new LocalDateInterval(idag.minusDays(19), idag.minusDays(10)))
            .getValue()).containsExactly(new InternalKravprioritet(1L, saksnummer, new JournalpostId("2"), 101L, BehandlingStatus.UTREDES, andreKrav));
    }

    @Test
    void skal_utlede_rekkefølge_blant_krav_på_tvers_av_saker() {
        var idag = LocalDate.now();
        var behandling1SøktePerioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(50), idag.minusDays(20)));

        var behandling2SøktePerioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(35), idag.minusDays(10)));

        var mottattDokumenter = List.of(new MottattKrav(new JournalpostId("1"), 100L), new MottattKrav(new JournalpostId("2"), 101L));
        var saksnummer = new Saksnummer("1");
        var saksnummer1 = new Saksnummer("2");
        var førsteKrav = LocalDateTime.now().minusDays(3);
        var behandlingStatus = Map.of(100L, new BehandlingMedMetadata(BehandlingStatus.AVSLUTTET, null), 101L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, null));
        var sakOgBehandling1 = new SakOgBehandlinger(1L, saksnummer, 100L,
            behandlingStatus, mottattDokumenter,
            Map.of(new KravDokument(new JournalpostId("1"), førsteKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling1SøktePerioder)));
        var andreKrav = LocalDateTime.now();
        var sakOgBehandling2 = new SakOgBehandlinger(2L, saksnummer1, 101L,
            behandlingStatus, mottattDokumenter,
            Map.of(new KravDokument(new JournalpostId("2"), andreKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling2SøktePerioder)));

        var kravRekkefølge = utleder.utledInternKravprio(List.of(sakOgBehandling1, sakOgBehandling2));

        assertThat(kravRekkefølge).hasSize(3);
        assertThat(kravRekkefølge.getSegment(new LocalDateInterval(idag.minusDays(50), idag.minusDays(36))).getValue())
            .containsExactly(new InternalKravprioritet(1L, saksnummer, new JournalpostId("1"), 100L, BehandlingStatus.AVSLUTTET, førsteKrav));

        assertThat(kravRekkefølge.getSegment(new LocalDateInterval(idag.minusDays(35), idag.minusDays(20)))
            .getValue()).containsExactly(new InternalKravprioritet(1L, saksnummer, new JournalpostId("1"), 100L, BehandlingStatus.AVSLUTTET, førsteKrav), new InternalKravprioritet(2L, saksnummer1, new JournalpostId("2"), 101L, BehandlingStatus.UTREDES, andreKrav));

        assertThat(kravRekkefølge.getSegment(new LocalDateInterval(idag.minusDays(19), idag.minusDays(10)))
            .getValue()).containsExactly(new InternalKravprioritet(2L, saksnummer1, new JournalpostId("2"), 101L, BehandlingStatus.UTREDES, andreKrav));
    }

    @Test
    void skal_utlede_en_kjøreplan_hvor_sak1_er_ferdig_behandlet_og_sak2_kan_behandles() {
        var førsteSak = new Saksnummer("1");
        var andreSak = new Saksnummer("2");

        var idag = LocalDate.now();
        var behandling1SøktePerioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(50), idag.minusDays(20)));

        var behandling2SøktePerioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(35), idag.minusDays(10)));

        var mottattDokumenter = List.of(new MottattKrav(new JournalpostId("1"), 100L), new MottattKrav(new JournalpostId("2"), 101L));
        var førsteKrav = LocalDateTime.now().minusDays(3);
        var behandlingStatus = Map.of(100L, new BehandlingMedMetadata(BehandlingStatus.AVSLUTTET, null), 101L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, null));
        var førsteSakOgBehandlinger = new SakOgBehandlinger(1L, førsteSak, 100L,
            behandlingStatus, mottattDokumenter,
            Map.of(new KravDokument(new JournalpostId("1"), førsteKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling1SøktePerioder)));
        var andreKrav = LocalDateTime.now();
        var andreSakOgBehandlinger = new SakOgBehandlinger(2L, andreSak, 101L,
            behandlingStatus, mottattDokumenter,
            Map.of(new KravDokument(new JournalpostId("2"), andreKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling2SøktePerioder)));

        var sakOgBehandlinger = List.of(førsteSakOgBehandlinger, andreSakOgBehandlinger);

        var input = new KravPrioInput(1L, førsteSak, Map.of(), LocalDateTimeline.empty(), sakOgBehandlinger);

        var kjøreplan = utleder.utledKravprioInternt(input);

        assertThat(kjøreplan).isNotNull();
        assertThat(kjøreplan.skalVentePåAnnenSak(1L)).isFalse();
        assertThat(kjøreplan.skalVentePåAnnenSak(2L)).isFalse();
    }

    @Test
    void skal_utlede_en_kjøreplan_hvor_sak1_er_kan_behandles_og_sak2_må_vente() {
        var førsteSak = new Saksnummer("1");
        var andreSak = new Saksnummer("2");

        var idag = LocalDate.now();
        var behandling1SøktePerioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(50), idag.minusDays(20)));

        var behandling2SøktePerioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(35), idag.minusDays(10)));

        var mottattDokumenter = List.of(new MottattKrav(new JournalpostId("1"), 100L), new MottattKrav(new JournalpostId("2"), 101L));
        var førsteKrav = LocalDateTime.now().minusDays(3);
        var behandlingStatus = Map.of(100L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, null), 101L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, null));
        var førsteSakOgBehandlinger = new SakOgBehandlinger(1L, førsteSak, 100L,
            behandlingStatus, mottattDokumenter,
            Map.of(new KravDokument(new JournalpostId("1"), førsteKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling1SøktePerioder)));
        var andreKrav = LocalDateTime.now();
        var andreSakOgBehandlinger = new SakOgBehandlinger(2L, andreSak, 101L,
            behandlingStatus, mottattDokumenter,
            Map.of(new KravDokument(new JournalpostId("2"), andreKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling2SøktePerioder)));

        var sakOgBehandlinger = List.of(førsteSakOgBehandlinger, andreSakOgBehandlinger);

        var input = new KravPrioInput(1L, førsteSak, Map.of(), LocalDateTimeline.empty(), sakOgBehandlinger);

        var kjøreplan = utleder.utledKravprioInternt(input);

        assertThat(kjøreplan).isNotNull();
        assertThat(kjøreplan.skalVentePåAnnenSak(1L)).isFalse();
        assertThat(kjøreplan.skalVentePåAnnenSak(2L)).isTrue();
    }

    @Test
    void skal_utlede_en_kjøreplan_med_gjensidig_avhengighet() {
        var førsteSak = new Saksnummer("1");
        var andreSak = new Saksnummer("2");

        var idag = LocalDate.now();
        var behandling1SøktePerioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(50), idag.minusDays(20)));

        var behandling2SøktePerioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(35), idag.minusDays(10)));
        var tilbakedatertePerioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(38), idag.minusDays(36)));

        var mottattDokumenter = List.of(new MottattKrav(new JournalpostId("1"), 100L), new MottattKrav(new JournalpostId("2"), 101L), new MottattKrav(new JournalpostId("3"), 101L));
        var førsteKrav = LocalDateTime.now().minusDays(3);
        var behandlingStatus = Map.of(100L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, null), 101L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, null));
        var førsteSakOgBehandlinger = new SakOgBehandlinger(1L, førsteSak, 100L,
            behandlingStatus, mottattDokumenter,
            Map.of(new KravDokument(new JournalpostId("1"), førsteKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling1SøktePerioder)));
        var andreKrav = LocalDateTime.now();
        var tilbakedatertKrav = LocalDateTime.now().minusDays(4);
        var andreSakOgBehandlinger = new SakOgBehandlinger(2L, andreSak, 101L,
            behandlingStatus, mottattDokumenter,
            Map.of(new KravDokument(new JournalpostId("2"), andreKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling2SøktePerioder),
                new KravDokument(new JournalpostId("3"), tilbakedatertKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(tilbakedatertePerioder)));

        var sakOgBehandlinger = List.of(førsteSakOgBehandlinger, andreSakOgBehandlinger);

        var input = new KravPrioInput(1L, førsteSak, Map.of(), LocalDateTimeline.empty(), sakOgBehandlinger);

        var kjøreplan = utleder.utledKravprioInternt(input);

        assertThat(kjøreplan).isNotNull();
        assertThat(kjøreplan.skalVentePåAnnenSak(1L)).isTrue();
        assertThat(kjøreplan.skalVentePåAnnenSak(2L)).isFalse();
        assertThat(kjøreplan.getPerioderSomSkalUtsettes(2L)).containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(35), idag.minusDays(20)));
    }

    @Test
    void skal_utlede_en_kjøreplan_trippel_med_gjensidig_avhengighet() {
        var førsteSak = new Saksnummer("1");
        var andreSak = new Saksnummer("2");
        var tredjeSak = new Saksnummer("3");

        var idag = LocalDate.now();
        var behandling1SøktePerioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(50), idag.minusDays(20)));

        var behandling2SøktePerioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(35), idag.minusDays(10)));
        var tilbakedatertePerioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(38), idag.minusDays(36)));
        var behandling3SøktePerioder = List.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(25), idag));

        var mottattDokumenterSak1 = List.of(new MottattKrav(new JournalpostId("1"), 100L));
        var mottattDokumenterSak2 = List.of(new MottattKrav(new JournalpostId("2"), 101L),
            new MottattKrav(new JournalpostId("3"), 101L));
        var mottattDokumenterSak3 = List.of(new MottattKrav(new JournalpostId("4"), 102L));


        var førsteKrav = LocalDateTime.now().minusDays(3);
        var behandlingStatus = Map.of(100L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, null), 101L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, null), 102L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, null));
        var førsteSakOgBehandlinger = new SakOgBehandlinger(1L, førsteSak, 100L,
            behandlingStatus, mottattDokumenterSak1,
            Map.of(new KravDokument(new JournalpostId("1"), førsteKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling1SøktePerioder)));
        var andreKrav = LocalDateTime.now();
        var tilbakedatertKrav = LocalDateTime.now().minusDays(4);
        var andreSakOgBehandlinger = new SakOgBehandlinger(2L, andreSak, 101L,
            behandlingStatus, mottattDokumenterSak2,
            Map.of(new KravDokument(new JournalpostId("2"), andreKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling2SøktePerioder),
                new KravDokument(new JournalpostId("3"), tilbakedatertKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(tilbakedatertePerioder)));
        var tredjeKrav = LocalDateTime.now().minusDays(1);
        var tredjeSakOgBehandlinger = new SakOgBehandlinger(3L, tredjeSak, 102L,
            behandlingStatus, mottattDokumenterSak3,
            Map.of(new KravDokument(new JournalpostId("4"), tredjeKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling3SøktePerioder)));

        var sakOgBehandlinger = List.of(førsteSakOgBehandlinger, andreSakOgBehandlinger, tredjeSakOgBehandlinger);

        var input = new KravPrioInput(1L, førsteSak, Map.of(), LocalDateTimeline.empty(), sakOgBehandlinger);

        var kjøreplan = utleder.utledKravprioInternt(input);

        assertThat(kjøreplan).isNotNull();
        assertThat(kjøreplan.skalVentePåAnnenSak(1L)).isTrue();
        assertThat(kjøreplan.skalVentePåAnnenSak(2L)).isFalse();
        assertThat(kjøreplan.getPerioderSomSkalUtsettes(2L)).containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(35), idag.minusDays(26)), DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(25), idag.minusDays(20)), DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(19), idag.minusDays(10)));
        assertThat(kjøreplan.skalVentePåAnnenSak(3L)).isFalse();
        assertThat(kjøreplan.getPerioderSomSkalUtsettes(3L)).containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(25), idag.minusDays(20)));

        // Steg 2 :: Etter vedtak på perioder som ikke har blitt utsatt fra fagsak 2 og 3
        behandlingStatus = Map.of(100L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, null),
            101L, new BehandlingMedMetadata(BehandlingStatus.AVSLUTTET, null),
            102L, new BehandlingMedMetadata(BehandlingStatus.AVSLUTTET, null),
            103L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, 101L),
            104L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, 102L));
        førsteSakOgBehandlinger = new SakOgBehandlinger(1L, førsteSak, 100L,
            behandlingStatus,
            mottattDokumenterSak1,
            Map.of(new KravDokument(new JournalpostId("1"), førsteKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling1SøktePerioder)));

        andreSakOgBehandlinger = new SakOgBehandlinger(2L, andreSak, 103L,
            behandlingStatus, mottattDokumenterSak2,
            Map.of(new KravDokument(new JournalpostId("2"), andreKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling2SøktePerioder),
                new KravDokument(new JournalpostId("3"), tilbakedatertKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(tilbakedatertePerioder)));
        tredjeSakOgBehandlinger = new SakOgBehandlinger(3L, tredjeSak, 104L,
            behandlingStatus, mottattDokumenterSak3,
            Map.of(new KravDokument(new JournalpostId("4"), tredjeKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling3SøktePerioder)));

        sakOgBehandlinger = List.of(førsteSakOgBehandlinger, andreSakOgBehandlinger, tredjeSakOgBehandlinger);
        Map<Long, NavigableSet<DatoIntervallEntitet>> utsattePerioderPerBehandling = Map.of(100L, new TreeSet<>(),
            101L, new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(35), idag.minusDays(26)), DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(25), idag.minusDays(20)), DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(19), idag.minusDays(10)))),
            102L, new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(idag.minusDays(25), idag.minusDays(20)))),
            103L, new TreeSet<>(),
            104L, new TreeSet<>());
        var oppdatertInput = new KravPrioInput(1L, førsteSak, utsattePerioderPerBehandling, LocalDateTimeline.empty(), sakOgBehandlinger);

        var oppdatertkjøreplan = utleder.utledKravprioInternt(oppdatertInput);

        assertThat(oppdatertkjøreplan).isNotNull();
        assertThat(oppdatertkjøreplan.skalVentePåAnnenSak(1L)).isFalse();
        assertThat(oppdatertkjøreplan.skalVentePåAnnenSak(2L)).isTrue();
        assertThat(oppdatertkjøreplan.skalVentePåAnnenSak(3L)).isTrue();

        // Steg 3 :: Etter vedtak på fagsak 1
        behandlingStatus = Map.of(100L, new BehandlingMedMetadata(BehandlingStatus.AVSLUTTET, null),
            101L, new BehandlingMedMetadata(BehandlingStatus.AVSLUTTET, null),
            102L, new BehandlingMedMetadata(BehandlingStatus.AVSLUTTET, null),
            103L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, 101L),
            104L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, 102L));
        førsteSakOgBehandlinger = new SakOgBehandlinger(1L, førsteSak, 100L,
            behandlingStatus,
            mottattDokumenterSak1,
            Map.of(new KravDokument(new JournalpostId("1"), førsteKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling1SøktePerioder)));

        andreSakOgBehandlinger = new SakOgBehandlinger(2L, andreSak, 103L,
            behandlingStatus, mottattDokumenterSak2,
            Map.of(new KravDokument(new JournalpostId("2"), andreKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling2SøktePerioder),
                new KravDokument(new JournalpostId("3"), tilbakedatertKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(tilbakedatertePerioder)));
        tredjeSakOgBehandlinger = new SakOgBehandlinger(3L, tredjeSak, 104L,
            behandlingStatus, mottattDokumenterSak3,
            Map.of(new KravDokument(new JournalpostId("4"), tredjeKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling3SøktePerioder)));

        sakOgBehandlinger = List.of(førsteSakOgBehandlinger, andreSakOgBehandlinger, tredjeSakOgBehandlinger);

        oppdatertInput = new KravPrioInput(1L, førsteSak, utsattePerioderPerBehandling, LocalDateTimeline.empty(), sakOgBehandlinger);

        oppdatertkjøreplan = utleder.utledKravprioInternt(oppdatertInput);

        assertThat(oppdatertkjøreplan).isNotNull();
        assertThat(oppdatertkjøreplan.skalVentePåAnnenSak(1L)).isFalse();
        assertThat(oppdatertkjøreplan.skalVentePåAnnenSak(2L)).isTrue();
        assertThat(oppdatertkjøreplan.skalVentePåAnnenSak(3L)).isFalse();
        assertThat(oppdatertkjøreplan.getPerioderSomSkalUtsettes(3L)).isEmpty();

        // Steg 4 :: Etter vedtak på fagsak 3
        behandlingStatus = Map.of(100L, new BehandlingMedMetadata(BehandlingStatus.AVSLUTTET, null),
            101L, new BehandlingMedMetadata(BehandlingStatus.AVSLUTTET, null),
            102L, new BehandlingMedMetadata(BehandlingStatus.AVSLUTTET, null),
            103L, new BehandlingMedMetadata(BehandlingStatus.UTREDES, 101L),
            104L, new BehandlingMedMetadata(BehandlingStatus.AVSLUTTET, 102L));
        førsteSakOgBehandlinger = new SakOgBehandlinger(1L, førsteSak, 100L,
            behandlingStatus,
            mottattDokumenterSak1,
            Map.of(new KravDokument(new JournalpostId("1"), førsteKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling1SøktePerioder)));

        andreSakOgBehandlinger = new SakOgBehandlinger(2L, andreSak, 103L,
            behandlingStatus, mottattDokumenterSak2,
            Map.of(new KravDokument(new JournalpostId("2"), andreKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling2SøktePerioder),
                new KravDokument(new JournalpostId("3"), tilbakedatertKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(tilbakedatertePerioder)));
        tredjeSakOgBehandlinger = new SakOgBehandlinger(3L, tredjeSak, 104L,
            behandlingStatus, mottattDokumenterSak3,
            Map.of(new KravDokument(new JournalpostId("4"), tredjeKrav, KravDokumentType.SØKNAD), søktePerioderTilVurdertePerioder(behandling3SøktePerioder)));

        sakOgBehandlinger = List.of(førsteSakOgBehandlinger, andreSakOgBehandlinger, tredjeSakOgBehandlinger);

        oppdatertInput = new KravPrioInput(1L, førsteSak, utsattePerioderPerBehandling, LocalDateTimeline.empty(), sakOgBehandlinger);

        oppdatertkjøreplan = utleder.utledKravprioInternt(oppdatertInput);

        assertThat(oppdatertkjøreplan).isNotNull();
        assertThat(oppdatertkjøreplan.skalVentePåAnnenSak(1L)).isFalse();
        assertThat(oppdatertkjøreplan.skalVentePåAnnenSak(2L)).isFalse();
        assertThat(oppdatertkjøreplan.getPerioderSomSkalUtsettes(2L)).isEmpty();
        assertThat(oppdatertkjøreplan.skalVentePåAnnenSak(3L)).isFalse();
        assertThat(oppdatertkjøreplan.getPerioderSomSkalUtsettes(3L)).isEmpty();
    }

    private List<VurdertSøktPeriode<Søknadsperiode>> søktePerioderTilVurdertePerioder(List<DatoIntervallEntitet> søktePerioder) {
        return søktePerioder.stream().map(it -> new VurdertSøktPeriode<>(it, Utfall.OPPFYLT, new Søknadsperiode(it))).toList();
    }
}
