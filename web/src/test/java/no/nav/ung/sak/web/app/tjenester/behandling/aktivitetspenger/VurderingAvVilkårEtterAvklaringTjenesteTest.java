package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderingAvVilkårPeriodeEtterAvklaringDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;
import no.nav.ung.sak.typer.Periode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tester den delte logikken som er felles for alle vilkår som vurderes etter en avklaring,
 * slik at de vilkårsspesifikke oppdatererne kun trenger å teste egen mapping og lagring.
 */
class VurderingAvVilkårEtterAvklaringTjenesteTest {

    private static final String SAKSBEHANDLER = "A111111";
    private static final long BEHANDLING_ID = 1234L;
    private static final VilkårType VILKÅR_TYPE = VilkårType.BOSTEDSVILKÅR;

    private static final Periode JANUAR = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final Periode MARS = new Periode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

    private VilkårResultatRepository vilkårResultatRepository;
    private VurderingAvVilkårEtterAvklaringTjeneste tjeneste;

    @BeforeAll
    static void beforeAll() {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker(SAKSBEHANDLER);
    }

    @AfterAll
    static void afterAll() {
        SubjectHandlerUtils.reset();
    }

    @BeforeEach
    void setUp() {
        vilkårResultatRepository = mock(VilkårResultatRepository.class);
        tjeneste = new VurderingAvVilkårEtterAvklaringTjeneste(vilkårResultatRepository);
    }

    @Test
    void ikke_oppfylt_periode_får_årsak_fra_avklaringen_og_ikke_fra_dto() {
        medVilkårsperioder(vilkårsperiode(JANUAR, Utfall.IKKE_VURDERT));

        var resultat = tjeneste.utled(BEHANDLING_ID, VILKÅR_TYPE,
            List.of(vurdering(JANUAR, false, "flyttet ut av Trondheim", null)),
            årsakTidslinje(JANUAR, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM));

        var vurdering = enesteSegment(resultat);
        assertThat(vurdering.godkjent()).isFalse();
        assertThat(vurdering.ikkeOppfyltÅrsak()).isEqualTo(BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM);
        assertThat(vurdering.erManuellVurdering()).isTrue();
        assertThat(vurdering.vurdertAv()).isEqualTo(SAKSBEHANDLER);
        assertThat(vurdering.begrunnelse()).isEqualTo("flyttet ut av Trondheim");
    }

    @Test
    void oppfylt_periode_får_ingen_ikkeOppfyltÅrsak() {
        medVilkårsperioder(vilkårsperiode(JANUAR, Utfall.IKKE_VURDERT));

        var resultat = tjeneste.utled(BEHANDLING_ID, VILKÅR_TYPE,
            List.of(vurdering(JANUAR, true, "oppfylt uansett årsak i avklaringen", null)),
            årsakTidslinje(JANUAR, BostedsvilkårIkkeOppfyltÅrsak.ANNET));

        var vurdering = enesteSegment(resultat);
        assertThat(vurdering.godkjent()).isTrue();
        assertThat(vurdering.ikkeOppfyltÅrsak()).isNull();
    }

    @Test
    void åpen_periode_lukkes_mot_seneste_vilkårsperiode() {
        medVilkårsperioder(vilkårsperiode(JANUAR, Utfall.IKKE_VURDERT), vilkårsperiode(MARS, Utfall.IKKE_VURDERT));

        var resultat = tjeneste.utled(BEHANDLING_ID, VILKÅR_TYPE,
            List.of(vurdering(new Periode(JANUAR.getFom(), null), false, "opphørt", null)),
            årsakTidslinje(new Periode(JANUAR.getFom(), MARS.getTom()), BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM));

        assertThat(resultat.getMaxLocalDate()).isEqualTo(MARS.getTom());
    }

    @Test
    void hull_i_vilkårsperiodene_etter_opphørsdato_fylles_ikke() {
        medVilkårsperioder(vilkårsperiode(JANUAR, Utfall.IKKE_VURDERT), vilkårsperiode(MARS, Utfall.IKKE_VURDERT));
        var opphørsdato = LocalDate.of(2026, 1, 15);

        var resultat = tjeneste.utled(BEHANDLING_ID, VILKÅR_TYPE,
            List.of(vurdering(new Periode(opphørsdato, null), false, "opphørt", null)),
            årsakTidslinje(new Periode(opphørsdato, MARS.getTom()), BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM));

        assertThat(resultat.getLocalDateIntervals())
            .extracting(it -> tuple(it.getFomDato(), it.getTomDato()))
            .containsExactly(
                tuple(opphørsdato, JANUAR.getTom()),
                tuple(MARS.getFom(), MARS.getTom()));
    }

    @Test
    void ikke_relevante_vilkårsperioder_vurderes_ikke() {
        medVilkårsperioder(vilkårsperiode(JANUAR, Utfall.IKKE_VURDERT), vilkårsperiode(MARS, Utfall.IKKE_RELEVANT));

        var resultat = tjeneste.utled(BEHANDLING_ID, VILKÅR_TYPE,
            List.of(vurdering(new Periode(JANUAR.getFom(), null), false, "opphørt", null)),
            årsakTidslinje(new Periode(JANUAR.getFom(), MARS.getTom()), BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM));

        assertThat(resultat.getMaxLocalDate()).isEqualTo(JANUAR.getTom());
    }

    @Test
    void kun_periodene_som_er_vurdert_gir_resultat() {
        medVilkårsperioder(vilkårsperiode(JANUAR, Utfall.IKKE_VURDERT), vilkårsperiode(MARS, Utfall.IKKE_VURDERT));

        var resultat = tjeneste.utled(BEHANDLING_ID, VILKÅR_TYPE,
            List.of(vurdering(JANUAR, false, "kun januar vurdert nå", null)),
            årsakTidslinje(JANUAR, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM)
                .crossJoin(årsakTidslinje(MARS, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM)));

        assertThat(resultat.getLocalDateIntervals())
            .extracting(it -> tuple(it.getFomDato(), it.getTomDato()))
            .containsExactly(tuple(JANUAR.getFom(), JANUAR.getTom()));
    }

    @Test
    void ingen_relevante_vilkårsperioder_gir_feil() {
        medVilkårsperioder(vilkårsperiode(JANUAR, Utfall.IKKE_RELEVANT));

        assertThatThrownBy(() -> tjeneste.utled(BEHANDLING_ID, VILKÅR_TYPE,
            List.of(vurdering(JANUAR, false, "uten relevante vilkårsperioder", null)),
            årsakTidslinje(JANUAR, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Fant ingen relevante vilkårsperioder");
    }

    @Test
    void ingen_foreslått_avklaring_gir_feil() {
        medVilkårsperioder(vilkårsperiode(JANUAR, Utfall.IKKE_VURDERT));

        assertThatThrownBy(() -> tjeneste.utled(BEHANDLING_ID, VILKÅR_TYPE,
            List.of(vurdering(JANUAR, false, "uten avklaring", null)),
            LocalDateTimeline.empty()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("uten at det finnes en ikkeOppfyltÅrsak");
    }

    @Test
    void vurdert_periode_utenfor_avklaringen_gir_feil() {
        medVilkårsperioder(vilkårsperiode(JANUAR, Utfall.IKKE_VURDERT), vilkårsperiode(MARS, Utfall.IKKE_VURDERT));

        assertThatThrownBy(() -> tjeneste.utled(BEHANDLING_ID, VILKÅR_TYPE,
            List.of(vurdering(MARS, false, "utenfor avklaringen", null)),
            årsakTidslinje(JANUAR, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ikke dekkes av en ikkeOppfyltÅrsak");
    }

    @Test
    void manglende_fritekst_når_årsaken_krever_det_gir_feil() {
        medVilkårsperioder(vilkårsperiode(JANUAR, Utfall.IKKE_VURDERT));

        assertThatThrownBy(() -> tjeneste.utled(BEHANDLING_ID, VILKÅR_TYPE,
            List.of(vurdering(JANUAR, false, "årsak annet uten fritekst", null)),
            årsakTidslinje(JANUAR, BostedsvilkårIkkeOppfyltÅrsak.ANNET)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fritekstVurderingBrev er påkrevd");
    }

    private void medVilkårsperioder(VilkårPeriodeBuilder... perioder) {
        var vilkårResultatBuilder = Vilkårene.builder();
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VILKÅR_TYPE);
        Arrays.stream(perioder).forEach(vilkårBuilder::leggTil);
        vilkårResultatBuilder.leggTil(vilkårBuilder);
        when(vilkårResultatRepository.hentHvisEksisterer(anyLong())).thenReturn(Optional.of(vilkårResultatBuilder.build()));
    }

    private static VilkårPeriodeBuilder vilkårsperiode(Periode periode, Utfall utfall) {
        return new VilkårPeriodeBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()))
            .medUtfall(utfall);
    }

    private static LocalDateTimeline<IkkeOppfyltDetaljertÅrsak> årsakTidslinje(Periode periode, IkkeOppfyltDetaljertÅrsak årsak) {
        return new LocalDateTimeline<>(List.of(new LocalDateSegment<>(periode.getFom(), periode.getTom(), årsak)));
    }

    private static VurderingAvVilkårPeriodeEtterAvklaringDto vurdering(Periode periode, boolean erVilkårOppfylt, String begrunnelse, String fritekstVurderingBrev) {
        return new VurderingAvVilkårPeriodeEtterAvklaringDto(new ÅpenPeriode(periode.getFom(), periode.getTom()), erVilkårOppfylt, begrunnelse, fritekstVurderingBrev);
    }

    private static VilkårsvurderingResultat enesteSegment(LocalDateTimeline<VilkårsvurderingResultat> resultat) {
        assertThat(resultat.getLocalDateIntervals()).hasSize(1);
        return resultat.stream().findFirst().orElseThrow().getValue();
    }
}
