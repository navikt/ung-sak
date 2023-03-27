package no.nav.k9.sak.web.app.tjenester.kravperioder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.krav.StatusForPerioderPåBehandling;
import no.nav.k9.sak.kontrakt.krav.ÅrsakTilVurdering;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.PeriodeMedÅrsak;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.JournalpostId;

class UtledStatusPåPerioderTjenesteTest {


    private UtledStatusPåPerioderTjeneste utledStatusPåPerioderTjeneste = new UtledStatusPåPerioderTjeneste(false);
    private LocalDate IDAG = LocalDate.now();
    private LocalDateTime NÅ = LocalDateTime.now();

    @Test
    void samme_inntektsmelding_revurdering() {
        var førstegangsscenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OMSORGSPENGER);
        Behandling behandling = førstegangsscenario
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();


        KravDokument kravDokTilkommetBehandling = new KravDokument(new JournalpostId("1"), NÅ, KravDokumentType.INNTEKTSMELDING);
        KravDokument kravDokTidligereBehandling = new KravDokument(new JournalpostId("2"), NÅ, KravDokumentType.INNTEKTSMELDING);

        LocalDate fom = IDAG.minusMonths(1);
        LocalDate tom = IDAG;

        var kravdokumenterMedPeriode = Map.of(
            kravDokTilkommetBehandling, List.of(byggSøktPeriode(fom, tom)),
            kravDokTidligereBehandling, List.of(byggSøktPeriode(fom, tom))
        );

        var perioderTilVurdering = new TreeSet<>(Set.of(DatoIntervallEntitet.fra(fom, tom)));

        NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles = new TreeSet<>();

        var revurderingPerioderFraAndreParter = new TreeSet<>(Set.of(new PeriodeMedÅrsak(DatoIntervallEntitet.fra(fom, tom), BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING)));

        StatusForPerioderPåBehandling utled = utledStatusPåPerioderTjeneste.utled(
            behandling,
            new PåTversAvHelgErKantIKantVurderer(),
            Set.of(kravDokTilkommetBehandling),
            kravdokumenterMedPeriode,
            perioderTilVurdering,
            perioderSomSkalTilbakestilles,
            revurderingPerioderFraAndreParter
        );

        assertThat(utled.getPerioderMedÅrsak().stream().findFirst().get().getÅrsaker()).containsOnly(ÅrsakTilVurdering.REVURDERER_NY_INNTEKTSMELDING);
    }

    @Test
    void samme_søknad_inntektsmelding_revurdering() {
        var førstegangsscenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        Behandling behandling = førstegangsscenario
            .medBehandlingType(BehandlingType.REVURDERING)
            .lagMocked();


        KravDokument søknadTilkommetBehandling = new KravDokument(new JournalpostId("1"), NÅ, KravDokumentType.SØKNAD);
        KravDokument inntektsmeldingTidligereBehandling = new KravDokument(new JournalpostId("3"), NÅ, KravDokumentType.INNTEKTSMELDING);


        LocalDate fom = IDAG.minusMonths(1);
        LocalDate tom = IDAG;

        var kravdokumenterMedPeriode = Map.of(
            søknadTilkommetBehandling, List.of(byggSøktPeriode(fom, tom)),
            inntektsmeldingTidligereBehandling, List.of(byggSøktPeriode(fom, tom))
        );

        var perioderTilVurdering = new TreeSet<>(Set.of(DatoIntervallEntitet.fra(fom, tom)));

        NavigableSet<DatoIntervallEntitet> perioderSomSkalTilbakestilles = new TreeSet<>();

        NavigableSet<PeriodeMedÅrsak> revurderingPerioderFraAndreParter = new TreeSet<>();

        StatusForPerioderPåBehandling utled = utledStatusPåPerioderTjeneste.utled(
            behandling,
            new PåTversAvHelgErKantIKantVurderer(),
            Set.of(søknadTilkommetBehandling),
            kravdokumenterMedPeriode,
            perioderTilVurdering,
            perioderSomSkalTilbakestilles,
            revurderingPerioderFraAndreParter
        );

        assertThat(utled.getPerioderMedÅrsak().stream().findFirst().get().getÅrsaker()).containsOnly(ÅrsakTilVurdering.ENDRING_FRA_BRUKER);
    }


    private SøktPeriode<VurdertSøktPeriode.SøktPeriodeData> byggSøktPeriode(LocalDate fom, LocalDate tom) {
        var dummyObjekt = new VurdertSøktPeriode.SøktPeriodeData() {
            @Override
            public <V> V getPayload() {
                return null;
            }
        };

        return new SøktPeriode<>(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), dummyObjekt);
    }


}
