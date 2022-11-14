package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPerioderHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.KursPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderHolder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

class OpplæringPeriodeSomTrengerVurderingUtlederTest {

    private final OpplæringPeriodeSomTrengerVurderingUtleder opplæringPeriodeSomTrengerVurderingUtleder = new OpplæringPeriodeSomTrengerVurderingUtleder();

    private final LocalDate dag1 = LocalDate.now().minusDays(1);
    private final LocalDate dag2 = LocalDate.now();
    private final DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(dag1, dag1);
    private final DatoIntervallEntitet periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(dag2, dag2);
    private final JournalpostId journalpostId1 = new JournalpostId("123");
    private final JournalpostId journalpostId2 = new JournalpostId("456");
    private Vilkårene vilkårene;
    private final VilkårResultatBuilder vilkårResultatBuilder = new VilkårResultatBuilder();
    private UttaksPerioderGrunnlag uttaksPerioderGrunnlag;

    @BeforeEach
    void setup() {
        UttakPerioderHolder uttakPerioderHolder = new UttakPerioderHolder(List.of(
            new PerioderFraSøknad(journalpostId1,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(lagKursperiode(periode1, "", null))),
            new PerioderFraSøknad(journalpostId2,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(lagKursperiode(periode2, "", null)))));
        uttaksPerioderGrunnlag = mock(UttaksPerioderGrunnlag.class);
        when(uttaksPerioderGrunnlag.getRelevantSøknadsperioder()).thenReturn(uttakPerioderHolder);
    }

    private KursPeriode lagKursperiode(DatoIntervallEntitet periode, String institusjon, UUID uuid) {
        return new KursPeriode(periode, null, null, institusjon, uuid, "beskrivelse");
    }

    private void setupVilkårsResultat(VilkårType vilkårType, Utfall utfall) {
        vilkårResultatBuilder.leggTil(new VilkårBuilder(vilkårType)
            .leggTil(new VilkårPeriodeBuilder().medPeriode(periode1).medUtfall(utfall))
            .leggTil(new VilkårPeriodeBuilder().medPeriode(periode2).medUtfall(utfall)));
    }

    @Test
    void ingenPerioderTrengerVurdering() {
        setupVilkårsResultat(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.IKKE_OPPFYLT);
        setupVilkårsResultat(VilkårType.LANGVARIG_SYKDOM, Utfall.IKKE_OPPFYLT);
        vilkårene = vilkårResultatBuilder.build();

        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = new TreeSet<>(List.of(periode1, periode2));

        var resultat = opplæringPeriodeSomTrengerVurderingUtleder.trengerVurderingFraSaksbehandler(perioderTilVurdering, vilkårene, null, uttaksPerioderGrunnlag);
        assertThat(resultat).isFalse();
    }

    @Test
    void ingenPerioderVurdert() {
        setupVilkårsResultat(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT);
        setupVilkårsResultat(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT);
        vilkårene = vilkårResultatBuilder.build();
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = new TreeSet<>(List.of(periode1));

        var resultat = opplæringPeriodeSomTrengerVurderingUtleder.trengerVurderingFraSaksbehandler(perioderTilVurdering, vilkårene, null, uttaksPerioderGrunnlag);
        assertThat(resultat).isTrue();
    }

    @Test
    void noenPerioderVurdert() {
        setupVilkårsResultat(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT);
        setupVilkårsResultat(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT);
        vilkårene = vilkårResultatBuilder.build();
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = new TreeSet<>(List.of(periode1, periode2));
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = new VurdertOpplæringGrunnlag(123L, null, null, new VurdertOpplæringPerioderHolder(List.of((new VurdertOpplæringPeriode(dag1, dag1, true, null, "")))));

        var resultat = opplæringPeriodeSomTrengerVurderingUtleder.trengerVurderingFraSaksbehandler(perioderTilVurdering, vilkårene, vurdertOpplæringGrunnlag, uttaksPerioderGrunnlag);
        assertThat(resultat).isTrue();
    }

    @Test
    void allePerioderVurdert() {
        setupVilkårsResultat(VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON, Utfall.OPPFYLT);
        setupVilkårsResultat(VilkårType.LANGVARIG_SYKDOM, Utfall.OPPFYLT);
        vilkårene = vilkårResultatBuilder.build();
        NavigableSet<DatoIntervallEntitet> perioderTilVurdering = new TreeSet<>(List.of(periode1, periode2));
        VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag = new VurdertOpplæringGrunnlag(123L, null, null,
            new VurdertOpplæringPerioderHolder(List.of(
                new VurdertOpplæringPeriode(dag1, dag1, true, null, ""),
                new VurdertOpplæringPeriode(dag2, dag2, true, null, ""))));

        var resultat = opplæringPeriodeSomTrengerVurderingUtleder.trengerVurderingFraSaksbehandler(perioderTilVurdering, vilkårene, vurdertOpplæringGrunnlag, uttaksPerioderGrunnlag);
        assertThat(resultat).isFalse();
    }
}
