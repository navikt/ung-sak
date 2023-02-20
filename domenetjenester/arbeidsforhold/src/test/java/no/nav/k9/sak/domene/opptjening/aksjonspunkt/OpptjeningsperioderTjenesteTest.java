package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.arbeidsforhold.RelatertYtelseTilstand;
import no.nav.k9.kodeverk.arbeidsforhold.TemaUnderkategori;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.organisasjon.Organisasjonstype;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvist;
import no.nav.k9.sak.domene.iay.modell.YtelseBuilder;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetVurderingOpptjeningsvilkår;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.typer.Stillingsprosent;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class OpptjeningsperioderTjenesteTest {

    private static final String ORG_NUMMER = "974760673";
    private InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.nyRef();
    private AktørId AKTØRID = AktørId.dummy();
    private LocalDate skjæringstidspunkt = LocalDate.now();

    @Inject
    private EntityManager entityManager;

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private IAYRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private VirksomhetTjeneste virksomhetTjeneste = Mockito.mock(VirksomhetTjeneste.class);
    private FagsakRepository fagsakRepository;
    private OpptjeningRepository opptjeningRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider = Mockito.mock(OppgittOpptjeningFilterProvider.class);
    private OpptjeningsperioderTjeneste opptjeningsperioderTjeneste;
    private OpptjeningAktivitetVurderingOpptjeningsvilkår vurderForVilkår;

    @BeforeEach
    public void setUp() {
        repositoryProvider = new IAYRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        fagsakRepository = new FagsakRepository(entityManager);
        opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        vurderForVilkår = new OpptjeningAktivitetVurderingOpptjeningsvilkår();
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);

        opptjeningsperioderTjeneste = new OpptjeningsperioderTjeneste(repositoryProvider.getOpptjeningRepository(), oppgittOpptjeningFilterProvider);
        when(oppgittOpptjeningFilterProvider.finnOpptjeningFilter(Mockito.anyLong())).thenReturn(new OppgittOpptjeningFilter() {
        });

        Virksomhet virksomhet = new Virksomhet.Builder().medOrgnr(ORG_NUMMER)
            .medOppstart(LocalDate.now())
            .medOrganisasjonstype(Organisasjonstype.VIRKSOMHET)
            .medRegistrert(LocalDate.now())
            .build();
        when(virksomhetTjeneste.hentOrganisasjon(ORG_NUMMER)).thenReturn(virksomhet);
    }

    @Test
    void skal_returnere_periode_for_arbeidsforhold_lagt_til_av_saksbehandler() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);

        ArbeidsforholdInformasjonBuilder informasjon = leggTilArbeidsforholdFraSaksbehandler(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt);
        iayTjeneste.lagreArbeidsforhold(behandling.getId(), AKTØRID, informasjon);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        // Act
        List<OpptjeningsperiodeForSaksbehandling> perioder = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(ref, iayGrunnlag, vurderForVilkår, opptjening.getOpptjeningPeriode(), DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt.plusMonths(6)), getYrkesaktivitetFilter(iayGrunnlag, ref.getAktørId()));

        // Assert
        assertThat(perioder.size()).isEqualTo(1);
        assertThat(perioder.get(0).getVurderingsStatus()).isEqualTo(VurderingsStatus.TIL_VURDERING);
    }

    @Test
    void skal_returnere_periode_for_arbeidsforhold_fra_AAREG() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(28), skjæringstidspunkt.minusDays(1), false);

        var inntektsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusYears(1), skjæringstidspunkt);
        var register = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, inntektsperiode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, Arbeidsgiver.virksomhet(ORG_NUMMER));
        iayTjeneste.lagreIayAggregat(behandling.getId(), register);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        // Act
        List<OpptjeningsperiodeForSaksbehandling> perioder = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(BehandlingReferanse.fra(behandling), iayGrunnlag, vurderForVilkår, opptjening.getOpptjeningPeriode(), DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt.plusMonths(6)), getYrkesaktivitetFilter(iayGrunnlag, behandling.getAktørId()));

        // Assert
        assertThat(perioder.size()).isEqualTo(1);
        assertThat(perioder.get(0).getVurderingsStatus()).isEqualTo(VurderingsStatus.TIL_VURDERING);
    }

    private YrkesaktivitetFilter getYrkesaktivitetFilter(InntektArbeidYtelseGrunnlag iayGrunnlag, AktørId aktørId) {
        return new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(aktørId)).før(skjæringstidspunkt);
    }

    @Test
    void skal_returnere_periode_for_arbeidsforhold_fra_AAREG_med_permisjon() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(28), skjæringstidspunkt.minusDays(1), false);

        var inntektsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusYears(1), skjæringstidspunkt);
        var permisjon = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusDays(25), skjæringstidspunkt.minusDays(5));
        var register = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, inntektsperiode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, Arbeidsgiver.virksomhet(ORG_NUMMER), permisjon);
        iayTjeneste.lagreIayAggregat(behandling.getId(), register);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        // Act
        List<OpptjeningsperiodeForSaksbehandling> perioder = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(BehandlingReferanse.fra(behandling), iayGrunnlag, vurderForVilkår, opptjening.getOpptjeningPeriode(), DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt.plusMonths(6)), getYrkesaktivitetFilter(iayGrunnlag, behandling.getAktørId()));

        // Assert
        assertThat(perioder.size()).isEqualTo(3);
        assertThat(perioder.get(0).getVurderingsStatus()).isEqualTo(VurderingsStatus.TIL_VURDERING);
        assertThat(perioder.get(1).getVurderingsStatus()).isEqualTo(VurderingsStatus.UNDERKJENT);
        assertThat(perioder.get(2).getVurderingsStatus()).isEqualTo(VurderingsStatus.TIL_VURDERING);
    }

    @Test
    void skal_returnere_periode_for_selvstendig_næringsdrivende() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(28), skjæringstidspunkt.minusDays(1), false);

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilEgneNæringer(Collections.singletonList(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
            .medVirksomhet(ORG_NUMMER)
            .medPeriode(DatoIntervallEntitet.fraOgMed(skjæringstidspunkt.minusMonths(1)))
            .medRegnskapsførerNavn("Børre Larsen")
            .medRegnskapsførerTlf("TELEFON")
            .medBegrunnelse("Hva mer?")));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        // Act
        List<OpptjeningsperiodeForSaksbehandling> perioder = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(BehandlingReferanse.fra(behandling), iayGrunnlag, vurderForVilkår, opptjening.getOpptjeningPeriode(), DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt.plusMonths(6)), getYrkesaktivitetFilter(iayGrunnlag, behandling.getAktørId()));

        // Assert
        assertThat(perioder.size()).isEqualTo(1);
        assertThat(perioder.get(0).getVurderingsStatus()).isEqualTo(VurderingsStatus.TIL_VURDERING);
    }

    @Test
    void skal_returnere_periode_for_frilanser() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(28), skjæringstidspunkt.minusDays(1), false);

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        var oppgittFrilans = OppgittOpptjeningBuilder.OppgittFrilansBuilder.ny().medErNyoppstartet(false).build();
        oppgitt.leggTilFrilansOpplysninger(oppgittFrilans);
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        var inntektsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusYears(1), skjæringstidspunkt);
        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, inntektsperiode, ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, Arbeidsgiver.virksomhet(ORG_NUMMER));
        opprettInntektForFrilanser(bekreftet, AKTØRID, ARBEIDSFORHOLD_ID, inntektsperiode, Arbeidsgiver.virksomhet(ORG_NUMMER));
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        // Act
        List<OpptjeningsperiodeForSaksbehandling> perioder = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(BehandlingReferanse.fra(behandling), iayGrunnlag, vurderForVilkår, opptjening.getOpptjeningPeriode(), DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt.plusMonths(6)), getYrkesaktivitetFilter(iayGrunnlag, behandling.getAktørId()));

        // Assert
        assertThat(perioder.size()).isEqualTo(1);
        assertThat(perioder.get(0).getVurderingsStatus()).isEqualTo(VurderingsStatus.TIL_VURDERING);
    }

    @Test
    void skal_returnere_periode_for_ytelse_som_gir_opptjening() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(28), skjæringstidspunkt.minusDays(1), false);

        var ytelsePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusYears(1), skjæringstidspunkt);
        InntektArbeidYtelseAggregatBuilder aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder aktørYtelseBuilder = aggregatBuilder.getAktørYtelseBuilder(AKTØRID);
        aktørYtelseBuilder.leggTilYtelse(byggYtelser(ytelsePeriode, Fagsystem.ARENA));
        aggregatBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), aggregatBuilder);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        // Act
        List<OpptjeningsperiodeForSaksbehandling> perioder = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(BehandlingReferanse.fra(behandling), iayGrunnlag, vurderForVilkår, opptjening.getOpptjeningPeriode(), DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt.plusMonths(6)), getYrkesaktivitetFilter(iayGrunnlag, behandling.getAktørId()));

        // Assert
        assertThat(perioder.size()).isEqualTo(1);
        assertThat(perioder.get(0).getVurderingsStatus()).isEqualTo(VurderingsStatus.TIL_VURDERING);
    }

    @Test
    void skal_returnere_sammenhengende_periode_for_ytelse_som_gir_opptjening() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(28), skjæringstidspunkt.minusDays(1), false);

        var ytelsePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusYears(1), skjæringstidspunkt);
        InntektArbeidYtelseAggregatBuilder aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder aktørYtelseBuilder = aggregatBuilder.getAktørYtelseBuilder(AKTØRID);
        aktørYtelseBuilder.leggTilYtelse(byggHelgeKnektYtelser(ytelsePeriode, Fagsystem.FPSAK));
        aggregatBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), aggregatBuilder);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        // Act
        List<OpptjeningsperiodeForSaksbehandling> perioder = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(BehandlingReferanse.fra(behandling), iayGrunnlag, vurderForVilkår, opptjening.getOpptjeningPeriode(), DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt.plusMonths(6)), getYrkesaktivitetFilter(iayGrunnlag, behandling.getAktørId()));

        // Assert
        assertThat(perioder.size()).isEqualTo(1);
        assertThat(perioder.get(0).getVurderingsStatus()).isEqualTo(VurderingsStatus.TIL_VURDERING);
    }

    @Test
    void skal_returnere_sammenhengende_periode_tom_søndag_for_ytelse_som_gir_opptjening_() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(28), skjæringstidspunkt.minusDays(1), false);

        var ytelsePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(1), skjæringstidspunkt);
        InntektArbeidYtelseAggregatBuilder aggregatBuilder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder aktørYtelseBuilder = aggregatBuilder.getAktørYtelseBuilder(AKTØRID);
        aktørYtelseBuilder.leggTilYtelse(byggHelgeKnektYtelserMedHull(ytelsePeriode, skjæringstidspunkt, Fagsystem.FPSAK, 2));
        aggregatBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), aggregatBuilder);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        // Act
        List<OpptjeningsperiodeForSaksbehandling> perioder = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(BehandlingReferanse.fra(behandling), iayGrunnlag, vurderForVilkår, opptjening.getOpptjeningPeriode(), DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt.plusMonths(6)), getYrkesaktivitetFilter(iayGrunnlag, behandling.getAktørId()));

        // Assert
        assertThat(perioder.size()).isEqualTo(2);
        assertThat(perioder.get(0).getVurderingsStatus()).isEqualTo(VurderingsStatus.TIL_VURDERING);
        assertThat(perioder.get(0).getPeriode().getTomDato().getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
    }

    @Test
    void skal_returnere_periode_for_annen_aktivitet() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(28), skjæringstidspunkt.minusDays(1), false);

        var aktivitetPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusYears(1), skjæringstidspunkt);
        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(aktivitetPeriode, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        // Act
        List<OpptjeningsperiodeForSaksbehandling> perioder = opptjeningsperioderTjeneste.mapPerioderForSaksbehandling(BehandlingReferanse.fra(behandling), iayGrunnlag, vurderForVilkår, opptjening.getOpptjeningPeriode(), DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt, skjæringstidspunkt.plusMonths(6)), getYrkesaktivitetFilter(iayGrunnlag, behandling.getAktørId()));

        // Assert
        assertThat(perioder.size()).isEqualTo(1);
        assertThat(perioder.get(0).getVurderingsStatus()).isEqualTo(VurderingsStatus.UNDERKJENT);
    }

    private YtelseBuilder byggYtelser(DatoIntervallEntitet periode,
                                      Fagsystem fagsystem) {
        YtelseBuilder ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medKilde(fagsystem)
            .medSaksnummer(new Saksnummer("123"))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato()))
            .medStatus(RelatertYtelseTilstand.LØPENDE)
            .medYtelseType(FagsakYtelseType.DAGPENGER)
            .medBehandlingsTema(TemaUnderkategori.UDEFINERT);
        byggYtelserAnvist(periode.getFomDato(), periode.getTomDato(), ytelseBuilder)
            .forEach(ytelseBuilder::medYtelseAnvist);
        return ytelseBuilder;
    }

    private YtelseBuilder byggHelgeKnektYtelser(DatoIntervallEntitet periode,
                                                Fagsystem fagsystem) {
        YtelseBuilder ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medKilde(fagsystem)
            .medSaksnummer(new Saksnummer("123"))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato()))
            .medStatus(RelatertYtelseTilstand.LØPENDE)
            .medYtelseType(FagsakYtelseType.FORELDREPENGER)
            .medBehandlingsTema(TemaUnderkategori.UDEFINERT);
        byggHelgeKnektePerioder(periode.getFomDato(), periode.getTomDato(), ytelseBuilder)
            .forEach(ytelseBuilder::medYtelseAnvist);
        return ytelseBuilder;
    }

    private YtelseBuilder byggHelgeKnektYtelserMedHull(DatoIntervallEntitet periode, LocalDate t1, Fagsystem fagsystem, int hullIUke) {
        YtelseBuilder ytelseBuilder = YtelseBuilder.oppdatere(Optional.empty())
            .medKilde(fagsystem)
            .medSaksnummer(new Saksnummer("123"))
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato()))
            .medStatus(RelatertYtelseTilstand.LØPENDE)
            .medYtelseType(FagsakYtelseType.FORELDREPENGER)
            .medBehandlingsTema(TemaUnderkategori.UDEFINERT);
        byggHelgeKnektePerioderMedHull(periode.getFomDato(), t1, ytelseBuilder, hullIUke)
            .forEach(ytelseBuilder::medYtelseAnvist);
        return ytelseBuilder;
    }

    private List<YtelseAnvist> byggHelgeKnektePerioderMedHull(LocalDate yaFom, LocalDate t1, YtelseBuilder ytelseBuilder, int hullIUke) {
        List<YtelseAnvist> ytelseAnvistList = new ArrayList<>();
        LocalDate fom = utledFom(yaFom);
        LocalDate tom = utledTom(fom);
        int i = 0;
        do {
            YtelseAnvist ya = ytelseBuilder.getAnvistBuilder()
                .medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
                .medUtbetalingsgradProsent(BigDecimal.valueOf(100L))
                .medBeløp(BigDecimal.valueOf(30000L))
                .medDagsats(BigDecimal.valueOf(1000L))
                .build();
            if (++i != hullIUke) {
                ytelseAnvistList.add(ya);
            }
            fom = utledFom(tom);
            tom = utledTom(fom);
        } while (tom.isBefore(t1));

        return ytelseAnvistList;
    }

    private List<YtelseAnvist> byggYtelserAnvist(LocalDate yaFom,
                                                 LocalDate t1,
                                                 YtelseBuilder ytelseBuilder) {
        // Man må sende meldekort hver 2 uker.
        final long ytelseDagerMellomrom = 13;
        List<YtelseAnvist> ytelseAnvistList = new ArrayList<>();
        LocalDate fom = yaFom;
        LocalDate tom = yaFom.plusDays(ytelseDagerMellomrom);
        do {
            YtelseAnvist ya = ytelseBuilder.getAnvistBuilder()
                .medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
                .medUtbetalingsgradProsent(BigDecimal.valueOf(100L))
                .medBeløp(BigDecimal.valueOf(30000L))
                .medDagsats(BigDecimal.valueOf(1000L))
                .build();
            ytelseAnvistList.add(ya);
            fom = tom.plusDays(1);
            tom = fom.plusDays(ytelseDagerMellomrom);
        } while (tom.isBefore(t1));

        return ytelseAnvistList;
    }

    private List<YtelseAnvist> byggHelgeKnektePerioder(LocalDate yaFom,
                                                       LocalDate t1,
                                                       YtelseBuilder ytelseBuilder) {
        List<YtelseAnvist> ytelseAnvistList = new ArrayList<>();
        LocalDate fom = utledFom(yaFom);
        LocalDate tom = utledTom(fom);
        do {
            YtelseAnvist ya = ytelseBuilder.getAnvistBuilder()
                .medAnvistPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
                .medUtbetalingsgradProsent(BigDecimal.valueOf(100L))
                .medBeløp(BigDecimal.valueOf(30000L))
                .medDagsats(BigDecimal.valueOf(1000L))
                .build();
            ytelseAnvistList.add(ya);
            fom = utledFom(tom);
            tom = utledTom(fom);
        } while (tom.isBefore(t1));

        return ytelseAnvistList;
    }

    private LocalDate utledFom(LocalDate fomDato) {
        if (DayOfWeek.FRIDAY.equals(fomDato.getDayOfWeek())) {
            return fomDato.plusDays(3);
        } else if (DayOfWeek.SATURDAY.equals(fomDato.getDayOfWeek())) {
            return fomDato.plusDays(2);
        } else if (DayOfWeek.SUNDAY.equals(fomDato.getDayOfWeek())) {
            return fomDato.plusDays(1);
        }
        return fomDato;
    }

    private LocalDate utledTom(LocalDate tomDato) {
        if (DayOfWeek.MONDAY.equals(tomDato.getDayOfWeek())) {
            return tomDato.plusDays(4);
        } else if (DayOfWeek.TUESDAY.equals(tomDato.getDayOfWeek())) {
            return tomDato.plusDays(3);
        } else if (DayOfWeek.WEDNESDAY.equals(tomDato.getDayOfWeek())) {
            return tomDato.plusDays(2);
        } else if (DayOfWeek.THURSDAY.equals(tomDato.getDayOfWeek())) {
            return tomDato.plusDays(1);
        }
        return tomDato;
    }


    private InntektArbeidYtelseAggregatBuilder opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AktørId aktørId, InternArbeidsforholdRef ref,
                                                                                                   DatoIntervallEntitet periode, ArbeidType type,
                                                                                                   Arbeidsgiver virksomhet, DatoIntervallEntitet permisjon) {
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);

        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);
        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(ref, virksomhet.getIdentifikator(), null), type);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder.medPeriode(periode);
        var ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder().medPeriode(periode);

        yrkesaktivitetBuilder
            .medArbeidType(type)
            .medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(ARBEIDSFORHOLD_ID)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode);

        if (permisjon != null) {
            var permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();
            yrkesaktivitetBuilder.leggTilPermisjon(permisjonBuilder.medPeriode(permisjon.getFomDato(), permisjon.getTomDato())
                .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.PERMITTERING)
                .medProsentsats(BigDecimal.valueOf(100))
                .build());
        }

        builder.leggTilAktørArbeid(aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder));

        return builder;
    }

    private InntektArbeidYtelseAggregatBuilder opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AktørId aktørId, InternArbeidsforholdRef ref,
                                                                                                   DatoIntervallEntitet periode, ArbeidType type,
                                                                                                   Arbeidsgiver virksomhet) {
        return opprettInntektArbeidYtelseAggregatForYrkesaktivitet(aktørId, ref, periode, type, virksomhet, null);
    }

    private void opprettInntektForFrilanser(InntektArbeidYtelseAggregatBuilder bekreftet, AktørId aktørId, InternArbeidsforholdRef ref, DatoIntervallEntitet periode,
                                            Arbeidsgiver virksomhet1) {
        var ainntektBuilder = bekreftet.getAktørInntektBuilder(aktørId);
        var inntektBuilder = ainntektBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING, new Opptjeningsnøkkel(ref, virksomhet1.getIdentifikator(), null));
        inntektBuilder.medArbeidsgiver(virksomhet1);
        inntektBuilder.leggTilInntektspost(InntektspostBuilder.ny().medInntektspostType(InntektspostType.LØNN)
            .medPeriode(periode.getFomDato(), periode.getTomDato()).medBeløp(BigDecimal.TEN));
        ainntektBuilder.leggTilInntekt(inntektBuilder);
        bekreftet.leggTilAktørInntekt(ainntektBuilder);
    }


    private ArbeidsforholdInformasjonBuilder leggTilArbeidsforholdFraSaksbehandler(LocalDate fraOgMed, LocalDate tilOgMed) {
        var arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);
        return ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty())
            .leggTil(ArbeidsforholdOverstyringBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver)
                .medHandling(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER)
                .leggTilOverstyrtPeriode(fraOgMed, tilOgMed)
                .medAngittStillingsprosent(new Stillingsprosent(BigDecimal.valueOf(100))));
    }

    private Behandling opprettBehandling(@SuppressWarnings("unused") LocalDate skjæringstidspunkt) {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.OMSORGSPENGER, AKTØRID);
        @SuppressWarnings("unused")
        Long fagsakId = fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        var nyttResultat = Vilkårene.builder().build();

        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        vilkårResultatRepository.lagre(behandling.getId(), nyttResultat);
        return behandling;
    }

}
