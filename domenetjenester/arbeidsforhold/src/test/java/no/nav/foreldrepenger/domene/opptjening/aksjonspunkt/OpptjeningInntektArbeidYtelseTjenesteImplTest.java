package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static no.nav.foreldrepenger.domene.arbeidsforhold.YtelseTestHelper.leggTilYtelse;
import static no.nav.foreldrepenger.domene.arbeidsforhold.YtelseTestHelper.opprettInntektArbeidYtelseAggregatForYrkesaktivitet;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningsperioderTjeneste;
import no.nav.foreldrepenger.domene.opptjening.VurderingsStatus;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class OpptjeningInntektArbeidYtelseTjenesteImplTest {

    public static final String ORG_NUMMER = "974760673";
    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final LocalDate skjæringstidspunkt = LocalDate.now();
    private IAYRepositoryProvider repositoryProvider = new IAYRepositoryProvider(repoRule.getEntityManager());
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private FagsakRepository fagsakRepository = new FagsakRepository(repoRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private OpptjeningRepository opptjeningRepository = repositoryProvider.getOpptjeningRepository();
    private VilkårResultatRepository vilkårResultatRepository = new VilkårResultatRepository(repoRule.getEntityManager());
    private VirksomhetTjeneste virksomhetTjeneste = new VirksomhetTjeneste(null, repositoryProvider.getVirksomhetRepository());
    private AksjonspunktutlederForVurderOppgittOpptjening apoOpptjening = new AksjonspunktutlederForVurderOppgittOpptjening(opptjeningRepository, iayTjeneste, virksomhetTjeneste);
    private AksjonspunktutlederForVurderBekreftetOpptjening apbOpptjening = new AksjonspunktutlederForVurderBekreftetOpptjening(opptjeningRepository, iayTjeneste);
    private OpptjeningsperioderTjeneste asdf = new OpptjeningsperioderTjeneste(iayTjeneste, repositoryProvider.getOpptjeningRepository(),
        apoOpptjening, apbOpptjening);
    private OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste = new OpptjeningInntektArbeidYtelseTjeneste(iayTjeneste, repositoryProvider.getOpptjeningRepository(), asdf);
    private InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.nyRef();
    private AktørId AKTØRID = AktørId.dummy();

    @Test
    public void skal_utlede_en_periode_for_egen_næring() {
        // Arrange
        final Behandling behandling = opprettBehandling(skjæringstidspunkt);

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));

        Virksomhet virksomhet = new VirksomhetEntitet.Builder()
            .medOrgnr(ORG_NUMMER)
            .medNavn("Virksomheten")
            .medRegistrert(LocalDate.now())
            .medOppstart(LocalDate.now())
            .oppdatertOpplysningerNå()
            .build();

        var virksomhetRepository = repositoryProvider.getVirksomhetRepository();
        virksomhetRepository.lagre(virksomhet);

        var oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilEgneNæringer(Collections.singletonList(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
            .medVirksomhet(ORG_NUMMER)
            .medPeriode(periode)
            .medRegnskapsførerNavn("Børre Larsen")
            .medRegnskapsførerTlf("TELEFON")
            .medBegrunnelse("Hva mer?")));

        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        // Assert
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        List<OpptjeningAktivitetPeriode> perioder = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, periode.getFomDato())
            .stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.NÆRING)).collect(Collectors.toList());

        assertThat(perioder).hasSize(1);
        OpptjeningAktivitetPeriode aktivitetPeriode = perioder.get(0);
        assertThat(aktivitetPeriode.getPeriode()).isEqualTo(periode);
        assertThat(aktivitetPeriode.getVurderingsStatus()).isEqualTo(VurderingsStatus.TIL_VURDERING);
    }

    @Test
    public void skal_sammenstille_grunnlag_og_overstyrt_deretter_utlede_opptjening_aktivitet_periode_for_vilkår_godkjent() {
        // Arrange
        final Behandling behandling = opprettBehandling(skjæringstidspunkt);
        var sisteLønnsendringsdato = skjæringstidspunkt;
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));

        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(
            AKTØRID, ARBEIDSFORHOLD_ID, periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, Arbeidsgiver.virksomhet(ORG_NUMMER),
            sisteLønnsendringsdato,
            VersjonType.REGISTER);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        // simulerer at det har blitt godkjent i GUI
        InntektArbeidYtelseAggregatBuilder saksbehandling = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(
            AKTØRID, ARBEIDSFORHOLD_ID, periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, Arbeidsgiver.virksomhet(ORG_NUMMER),
            sisteLønnsendringsdato,
            VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandling);

        // Act
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        List<OpptjeningAktivitetPeriode> perioder = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, skjæringstidspunkt);
        assertThat(perioder).hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.FERDIG_VURDERT_GODKJENT)).collect(Collectors.toList()))
            .hasSize(1);
    }

    @Test
    public void skal_sammenstille_grunnlag_og_overstyrt_deretter_utlede_opptjening_aktivitet_periode_for_vilkår_underkjent() {
        // Arrange
        LocalDate iDag = LocalDate.now();
        final Behandling behandling = opprettBehandling(iDag);
        var sisteLønnsendringsdato = skjæringstidspunkt;
        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(3), iDag.minusMonths(2));

        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(ORG_NUMMER);
        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, virksomhet,
            sisteLønnsendringsdato,
            VersjonType.REGISTER);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        // simulerer at det har blitt underkjent i GUI
        InntektArbeidYtelseAggregatBuilder overstyrt = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.of(bekreftet.build()), VersjonType.SAKSBEHANDLET);

        YrkesaktivitetBuilder yrkesaktivitetBuilder = overstyrt.getAktørArbeidBuilder(AKTØRID)
            .getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(ARBEIDSFORHOLD_ID, ORG_NUMMER, null),
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilder.tilbakestillAvtaler();
        iayTjeneste.lagreIayAggregat(behandling.getId(), overstyrt);

        // Act
        BehandlingReferanse behandlingReferanse = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        List<OpptjeningAktivitetPeriode> perioder = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(behandlingReferanse, skjæringstidspunkt);
        assertThat(perioder).hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.FERDIG_VURDERT_UNDERKJENT)).collect(Collectors.toList()))
            .hasSize(1);
    }

    @Test
    public void skal_hente_ytelse_før_stp() {
        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        var sisteLønnsendringsdato = skjæringstidspunkt;

        final Behandling behandling = scenario.lagre(repositoryProvider);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt);

        InntektArbeidYtelseAggregatBuilder builder = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(
            søkerAktørId, ARBEIDSFORHOLD_ID, periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, Arbeidsgiver.virksomhet(ORG_NUMMER),
            sisteLønnsendringsdato,
            VersjonType.REGISTER);

        builder.leggTilAktørYtelse(leggTilYtelse(builder.getAktørYtelseBuilder(søkerAktørId), skjæringstidspunkt.minusDays(20), skjæringstidspunkt.minusDays(10),
            RelatertYtelseTilstand.AVSLUTTET, "12342234", FagsakYtelseType.FORELDREPENGER));
        builder.leggTilAktørYtelse(leggTilYtelse(builder.getAktørYtelseBuilder(søkerAktørId), skjæringstidspunkt.minusDays(3), skjæringstidspunkt.minusDays(1),
            RelatertYtelseTilstand.LØPENDE, "1222433", FagsakYtelseType.SYKEPENGER));

        Long behandlingId = behandling.getId();
        iayTjeneste.lagreIayAggregat(behandlingId, builder);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusDays(30), skjæringstidspunkt, false);

        // Act
        Optional<Ytelse> sisteYtelseOpt = opptjeningTjeneste.hentSisteInfotrygdYtelseFørSkjæringstidspunktForOpptjening(behandlingId, søkerAktørId);

        // Assert
        assertThat(sisteYtelseOpt.isPresent()).isTrue();
        assertThat(sisteYtelseOpt.get().getSaksnummer().getVerdi()).isEqualTo("1222433");
    }

    @Test
    public void skal_lage_sammehengende_liste() {
        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        var sisteLønnsendringsdato = skjæringstidspunkt;
        final Behandling behandling = scenario.lagre(repositoryProvider);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt);


        var builder = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(
            søkerAktørId, ARBEIDSFORHOLD_ID, periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, Arbeidsgiver.virksomhet(ORG_NUMMER),
            sisteLønnsendringsdato,
            VersjonType.REGISTER);

        builder.leggTilAktørYtelse(leggTilYtelse(builder.getAktørYtelseBuilder(søkerAktørId), skjæringstidspunkt.minusDays(10), skjæringstidspunkt.minusDays(2),
            RelatertYtelseTilstand.LØPENDE, "12342234", FagsakYtelseType.SYKEPENGER));
        builder.leggTilAktørYtelse(leggTilYtelse(builder.getAktørYtelseBuilder(søkerAktørId), skjæringstidspunkt.minusDays(15), skjæringstidspunkt.minusDays(11),
            RelatertYtelseTilstand.AVSLUTTET, "1222433", FagsakYtelseType.SYKEPENGER));
        builder.leggTilAktørYtelse(leggTilYtelse(builder.getAktørYtelseBuilder(søkerAktørId), skjæringstidspunkt.minusDays(20), skjæringstidspunkt.minusDays(16),
            RelatertYtelseTilstand.AVSLUTTET, "124234", FagsakYtelseType.SYKEPENGER));
        builder.leggTilAktørYtelse(leggTilYtelse(builder.getAktørYtelseBuilder(søkerAktørId), skjæringstidspunkt.minusDays(30), skjæringstidspunkt.minusDays(22),
            RelatertYtelseTilstand.AVSLUTTET, "123253254", FagsakYtelseType.SYKEPENGER));

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusDays(30), skjæringstidspunkt, false);

        // Act
        Collection<Ytelse> sammenhengendeYtelser = opptjeningTjeneste.hentSammenhengendeInfotrygdYtelserFørSkjæringstidspunktForOppjening(behandling.getId(), behandling.getAktørId());

        // Assert
        assertThat(sammenhengendeYtelser).hasSize(3);
        assertThat(sammenhengendeYtelser.stream().map(s -> s.getSaksnummer().getVerdi()).collect(Collectors.toList())).containsOnly("12342234", "1222433",
            "124234");
    }

    @Test
    public void skal_lage_sammehengende_liste_med_overlappende_ytelser() {
        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        var sisteLønnsendringsdato = skjæringstidspunkt;
        final Behandling behandling = scenario.lagre(repositoryProvider);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt);


        InntektArbeidYtelseAggregatBuilder builder = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(
            søkerAktørId, ARBEIDSFORHOLD_ID, periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, Arbeidsgiver.virksomhet(ORG_NUMMER),
            sisteLønnsendringsdato,
            VersjonType.REGISTER);

        builder.leggTilAktørYtelse(
            leggTilYtelse(builder.getAktørYtelseBuilder(søkerAktørId), skjæringstidspunkt.minusDays(30), skjæringstidspunkt.plusDays(10), RelatertYtelseTilstand.LØPENDE, "12342234", FagsakYtelseType.SYKEPENGER));
        builder.leggTilAktørYtelse(
            leggTilYtelse(builder.getAktørYtelseBuilder(søkerAktørId), skjæringstidspunkt.minusDays(29), skjæringstidspunkt.minusDays(20), RelatertYtelseTilstand.AVSLUTTET, "1222433", FagsakYtelseType.SYKEPENGER));
        builder.leggTilAktørYtelse(
            leggTilYtelse(builder.getAktørYtelseBuilder(søkerAktørId), skjæringstidspunkt.minusDays(19), skjæringstidspunkt.minusDays(10), RelatertYtelseTilstand.AVSLUTTET, "124234", FagsakYtelseType.SYKEPENGER));
        builder.leggTilAktørYtelse(
            leggTilYtelse(builder.getAktørYtelseBuilder(søkerAktørId), skjæringstidspunkt.minusDays(9), skjæringstidspunkt.minusDays(1), RelatertYtelseTilstand.AVSLUTTET, "123253254", FagsakYtelseType.SYKEPENGER));

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusDays(30), skjæringstidspunkt, false);

        // Act
        Collection<Ytelse> sammenhengendeYtelser = opptjeningTjeneste.hentSammenhengendeInfotrygdYtelserFørSkjæringstidspunktForOppjening(behandling.getId(), behandling.getAktørId());

        // Assert
        assertThat(sammenhengendeYtelser).hasSize(4);
        assertThat(sammenhengendeYtelser.stream().map(s -> s.getSaksnummer().getVerdi()).collect(Collectors.toList())).containsOnly("12342234", "1222433",
            "124234", "123253254");
    }

    @Test
    public void skal_lage_sammehengende_liste_med_delvis_overlappende_ytelser() {
        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        var sisteLønnsendringsdato = skjæringstidspunkt;

        final Behandling behandling = scenario.lagre(repositoryProvider);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt);


        InntektArbeidYtelseAggregatBuilder builder = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(
            søkerAktørId, ARBEIDSFORHOLD_ID, periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, Arbeidsgiver.virksomhet(ORG_NUMMER),
            sisteLønnsendringsdato,
            VersjonType.REGISTER);

        builder.leggTilAktørYtelse(
            leggTilYtelse(builder.getAktørYtelseBuilder(søkerAktørId), skjæringstidspunkt.minusDays(10), skjæringstidspunkt.plusDays(10), RelatertYtelseTilstand.LØPENDE, "12342234", FagsakYtelseType.SYKEPENGER));
        builder.leggTilAktørYtelse(
            leggTilYtelse(builder.getAktørYtelseBuilder(søkerAktørId), skjæringstidspunkt.minusDays(20), skjæringstidspunkt.minusDays(10), RelatertYtelseTilstand.AVSLUTTET, "1222433", FagsakYtelseType.SYKEPENGER));

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusDays(30), skjæringstidspunkt, false);

        // Act
        Collection<Ytelse> sammenhengendeYtelser = opptjeningTjeneste.hentSammenhengendeInfotrygdYtelserFørSkjæringstidspunktForOppjening(behandling.getId(), behandling.getAktørId());

        // Assert
        assertThat(sammenhengendeYtelser).hasSize(2);
        assertThat(sammenhengendeYtelser.stream().map(s -> s.getSaksnummer().getVerdi()).collect(Collectors.toList())).containsOnly("12342234", "1222433");
    }

    @Test
    public void skal_ikke_lage_sammehengende_liste_med_1_dag_mellom_ytelser() {
        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        var sisteLønnsendringsdato = skjæringstidspunkt;
        final Behandling behandling = scenario.lagre(repositoryProvider);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt);

        var builder = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(
            søkerAktørId, ARBEIDSFORHOLD_ID, periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, Arbeidsgiver.virksomhet(ORG_NUMMER),
            sisteLønnsendringsdato,
            VersjonType.REGISTER);

        builder.leggTilAktørYtelse(
            leggTilYtelse(builder.getAktørYtelseBuilder(søkerAktørId), skjæringstidspunkt.minusDays(10), skjæringstidspunkt.plusDays(10), RelatertYtelseTilstand.LØPENDE, "12342234", FagsakYtelseType.SYKEPENGER));
        builder.leggTilAktørYtelse(
            leggTilYtelse(builder.getAktørYtelseBuilder(søkerAktørId), skjæringstidspunkt.minusDays(20), skjæringstidspunkt.minusDays(12), RelatertYtelseTilstand.AVSLUTTET, "1222433", FagsakYtelseType.SYKEPENGER));

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusDays(30), skjæringstidspunkt, false);

        // Act
        Collection<Ytelse> sammenhengendeYtelser = opptjeningTjeneste.hentSammenhengendeInfotrygdYtelserFørSkjæringstidspunktForOppjening(behandling.getId(), behandling.getAktørId());

        // Assert
        assertThat(sammenhengendeYtelser).hasSize(1);
        assertThat(sammenhengendeYtelser.stream().map(s -> s.getSaksnummer().getVerdi()).collect(Collectors.toList())).containsOnly("12342234");
    }

    @Test
    public void skal_lage_tom_sammehengende_liste_uten_ytelser() {
        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        var sisteLønnsendringsdato = skjæringstidspunkt;
        Behandling behandling = scenario.lagre(repositoryProvider);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt);

        var builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.SAKSBEHANDLET);


        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(
            søkerAktørId, ARBEIDSFORHOLD_ID, periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, Arbeidsgiver.virksomhet(ORG_NUMMER),
            sisteLønnsendringsdato,
            VersjonType.REGISTER);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        // simulerer at det har blitt godkjent i GUI
        var saksbehandling = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(
            søkerAktørId, ARBEIDSFORHOLD_ID, periode, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ZERO, Arbeidsgiver.virksomhet(ORG_NUMMER),
            sisteLønnsendringsdato,
            VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandling);

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusDays(30), skjæringstidspunkt, false);

        // Act
        Collection<Ytelse> sammenhengendeYtelser = opptjeningTjeneste.hentSammenhengendeInfotrygdYtelserFørSkjæringstidspunktForOppjening(behandling.getId(), behandling.getAktørId());

        // Assert
        assertThat(sammenhengendeYtelser).isEmpty();
    }

    private Behandling opprettBehandling(LocalDate iDag) {
        Personinfo personinfo = new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AKTØRID)
            .medFødselsdato(iDag.minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12312312312"))
            .medForetrukketSpråk(Språkkode.nb)
            .build();
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo));
        fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = builder.build();
        Behandlingsresultat.opprettFor(behandling);
        Vilkårene nyttResultat = Vilkårene.builder().build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        vilkårResultatRepository.lagre(behandling.getId(), nyttResultat);

        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(10), skjæringstidspunkt, false);
        return behandling;
    }
}
