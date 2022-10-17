package no.nav.k9.sak.web.app.tjenester;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import no.nav.k9.prosesstask.rest.ProsessTaskRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.abakus.IAYRegisterdataCallbackRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.aktør.AktørRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingBackendRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt.ForvaltningAksjonspunktSammendragRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold.ArbeidsgiverRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.arbeidsforhold.InntektArbeidYtelseRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag.BeregningsgrunnlagRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag.ForvaltningBeregningRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.beregningsresultat.BeregningsresultatRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.beregningsresultat.OverlapendeYtelserRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.død.RettVedDødRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.historikk.HistorikkRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.kompletthet.KompletthetForBeregningRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.kontroll.KontrollRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.omsorg.OmsorgenForRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.omsorgspenger.FosterbarnRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.omsorgspenger.RammevedtakRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.omsorgspenger.ÅrskvantumRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.opptjening.OpptjeningRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.personopplysning.ForvaltningPersonRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.personopplysning.PleietrengendeRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.sykdom.SykdomRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.sykdom.SykdomVurderingRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.sykdom.dokument.PleietrengendeSykdomDokumentRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.søknadsfrist.SøknadsfristRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.tilsyn.VurderTilsynRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.uttak.PleiepengerUttakRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.uttak.UtenlandsoppholdRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.uttak.UttakRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.vedtak.DokumenterMedUstrukturerteDataRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.vedtak.TotrinnskontrollRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.vilkår.VilkårRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.brev.BrevRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.fordeling.FordelHendelseRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.fordeling.FordelRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.forvaltning.DiagnostikkRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.forvaltning.ForvaltningInfotrygMigreringRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.forvaltning.ForvaltningMidlertidigDriftRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.forvaltning.ForvaltningOppdragRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.forvaltning.rapportering.RapporteringRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.integrasjonstatus.IntegrasjonstatusRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.kodeverk.KodeverkRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.kravperioder.PerioderTilBehandlingMedKildeRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.los.LosRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.opplæringsinstitusjon.GodkjentOpplæringsinstitusjonRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.punsj.PunsjRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.register.RedirectToRegisterRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.saksbehandler.InitielleLinksRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.saksbehandler.NavAnsattRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.saksbehandler.SaksbehandlerRestTjeneste;
import no.nav.k9.sak.web.server.abac.PipRestTjeneste;

public class RestImplementationClasses {

    public Collection<Class<?>> getImplementationClasses() {
        Set<Class<?>> classes = new HashSet<>();

        classes.add(FagsakRestTjeneste.class);
        classes.add(NavAnsattRestTjeneste.class);
        classes.add(SaksbehandlerRestTjeneste.class);
        classes.add(InitielleLinksRestTjeneste.class);
        classes.add(BehandlingRestTjeneste.class);
        classes.add(BehandlingBackendRestTjeneste.class);
        classes.add(BeregningsgrunnlagRestTjeneste.class);
        classes.add(AksjonspunktRestTjeneste.class);
        classes.add(DokumentRestTjeneste.class);
        classes.add(KompletthetForBeregningRestTjeneste.class);
        classes.add(HistorikkRestTjeneste.class);
        classes.add(KodeverkRestTjeneste.class);
        classes.add(OmsorgenForRestTjeneste.class);
        classes.add(SøknadsfristRestTjeneste.class);
        classes.add(FordelRestTjeneste.class);
        classes.add(FordelHendelseRestTjeneste.class);
        classes.add(BeregningsresultatRestTjeneste.class);
        classes.add(TotrinnskontrollRestTjeneste.class);
        classes.add(PerioderTilBehandlingMedKildeRestTjeneste.class);
        classes.add(ÅrskvantumRestTjeneste.class);
        classes.add(RammevedtakRestTjeneste.class);
        classes.add(FosterbarnRestTjeneste.class);
        classes.add(PersonRestTjeneste.class);
        classes.add(SøknadRestTjeneste.class);
        classes.add(OpptjeningRestTjeneste.class);
        classes.add(InntektArbeidYtelseRestTjeneste.class);
        classes.add(ArbeidsgiverRestTjeneste.class);
        classes.add(VilkårRestTjeneste.class);
        classes.add(IntegrasjonstatusRestTjeneste.class);
        classes.add(PipRestTjeneste.class);
        classes.add(TilbakekrevingRestTjeneste.class);
        classes.add(AktørRestTjeneste.class);
        classes.add(SykdomRestTjeneste.class);
        classes.add(SykdomVurderingRestTjeneste.class);
        classes.add(PleietrengendeSykdomDokumentRestTjeneste.class);
        classes.add(DokumenterMedUstrukturerteDataRestTjeneste.class);
        classes.add(KontrollRestTjeneste.class);
        classes.add(IAYRegisterdataCallbackRestTjeneste.class);
        classes.add(UttakRestTjeneste.class);
        classes.add(PleiepengerUttakRestTjeneste.class);
        classes.add(UtenlandsoppholdRestTjeneste.class);
        classes.add(VurderTilsynRestTjeneste.class);
        classes.add(RettVedDødRestTjeneste.class);
        classes.add(PleietrengendeRestTjeneste.class);
        classes.add(PunsjRestTjeneste.class);
        classes.add(OverlapendeYtelserRestTjeneste.class);
        classes.add(RedirectToRegisterRestTjeneste.class);
        classes.add(LosRestTjeneste.class);
        classes.add(GodkjentOpplæringsinstitusjonRestTjeneste.class);

        // Forvaltningstjenester - fjernes løpende
        classes.add(ProsessTaskRestTjeneste.class);
        classes.add(ForvaltningAksjonspunktSammendragRestTjeneste.class);
        classes.add(ForvaltningMidlertidigDriftRestTjeneste.class);
        classes.add(ForvaltningOppdragRestTjeneste.class);
        classes.add(ForvaltningBeregningRestTjeneste.class);
        classes.add(ForvaltningInfotrygMigreringRestTjeneste.class);
        classes.add(ForvaltningPersonRestTjeneste.class);
        classes.add(DiagnostikkRestTjeneste.class);
        classes.add(RapporteringRestTjeneste.class);

        deprecatedServicesForBrev(classes);

        return Set.copyOf(classes);
    }

    /***
     * @deprecated fjernees når abakus har tatt over
     */
    @Deprecated
    private void deprecatedServicesForBrev(Set<Class<?>> classes) {
        classes.add(BrevRestTjeneste.class);
    }

}
