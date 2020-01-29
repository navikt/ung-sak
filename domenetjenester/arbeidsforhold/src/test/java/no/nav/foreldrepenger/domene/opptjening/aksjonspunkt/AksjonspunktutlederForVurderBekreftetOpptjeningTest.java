package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;

public class AksjonspunktutlederForVurderBekreftetOpptjeningTest {

    private final AktørId AKTØRID = AktørId.dummy();
    private final LocalDate skjæringstidspunkt = LocalDate.now();

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private AksjonspunktutlederForVurderBekreftetOpptjening aksjonspunktutleder;
    private FagsakRepository fagsakRepository = repositoryProvider.getFagsakRepository();
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();

    @Before
    public void setUp() {
        aksjonspunktutleder = new AksjonspunktutlederForVurderBekreftetOpptjening(repositoryProvider.getOpptjeningRepository(), iayTjeneste);

    }

    @Test
    public void skal_gi_aksjonspunkt_for_fiktivt_arbeidsforhold() {
        // Arrange
        Behandling behandling = opprettBehandling(skjæringstidspunkt);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt);
        iayTjeneste.lagreArbeidsforhold(behandling.getId(), AKTØRID, ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty())
            .leggTil(ArbeidsforholdOverstyringBuilder
                .oppdatere(Optional.empty())
                .leggTilOverstyrtPeriode(periode.getFomDato(), periode.getTomDato())
                .medAngittStillingsprosent(new Stillingsprosent(100))
                .medArbeidsforholdRef(InternArbeidsforholdRef.nullRef())
                .medArbeidsgiver(Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG))
                .medAngittArbeidsgiverNavn("Ambassade")));
        InntektArbeidYtelseGrunnlag iayGrunnlag = iayTjeneste.finnGrunnlag(behandling.getId()).get();
        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), (Yrkesaktivitet) null);
        Yrkesaktivitet overstyrt = filter.getYrkesaktiviteter().iterator().next();
        // Act
        boolean girAksjonspunkt = aksjonspunktutleder.girAksjonspunktForArbeidsforhold(filter, behandling.getId(), null, overstyrt);
        // Assert
        assertThat(girAksjonspunkt).isTrue();
    }

    private Behandling opprettBehandling(LocalDate iDag) {
        final Personinfo personinfo = new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AKTØRID)
            .medFødselsdato(iDag.minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12312312312"))
            .medForetrukketSpråk(Språkkode.nb)
            .build();
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo));
        fagsakRepository.opprettNy(fagsak);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();
        Behandlingsresultat.opprettFor(behandling);
        final Vilkårene nyttResultat = Vilkårene.builder().build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        repositoryProvider.getVilkårResultatRepository().lagre(behandling.getId(), nyttResultat);

        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(10), skjæringstidspunkt, false);
        return behandling;
    }
}
