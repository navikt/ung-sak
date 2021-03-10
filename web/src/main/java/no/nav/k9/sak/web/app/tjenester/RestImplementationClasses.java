package no.nav.k9.sak.web.app.tjenester;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
import no.nav.k9.sak.web.app.tjenester.behandling.historikk.HistorikkRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.kontroll.KontrollRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.omsorg.OmsorgenForRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.omsorgspenger.RammevedtakRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.opptjening.OpptjeningRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.sykdom.SykdomRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.sykdom.SykdomVurderingRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.sykdom.dokument.SykdomDokumentRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.uttak.PleiepengerUttakRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.uttak.UttakRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.vedtak.TotrinnskontrollRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.vilkår.VilkårRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.omsorgspenger.ÅrskvantumRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.brev.BrevRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.fordeling.FordelRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.forvaltning.ForvaltningMidlertidigDriftRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.forvaltning.ForvaltningOppdragRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.integrasjonstatus.IntegrasjonstatusRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.kodeverk.KodeverkRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.saksbehandler.InitielleLinksRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.saksbehandler.NavAnsattRestTjeneste;
import no.nav.k9.sak.web.server.abac.PipRestTjeneste;
import no.nav.k9.prosesstask.rest.ProsessTaskRestTjeneste;

public class RestImplementationClasses {

    public Collection<Class<?>> getImplementationClasses() {
        Set<Class<?>> classes = new HashSet<>();

        classes.add(FagsakRestTjeneste.class);
        classes.add(NavAnsattRestTjeneste.class);
        classes.add(InitielleLinksRestTjeneste.class);
        classes.add(BehandlingRestTjeneste.class);
        classes.add(BehandlingBackendRestTjeneste.class);
        classes.add(BeregningsgrunnlagRestTjeneste.class);
        classes.add(AksjonspunktRestTjeneste.class);
        classes.add(DokumentRestTjeneste.class);
        classes.add(HistorikkRestTjeneste.class);
        classes.add(KodeverkRestTjeneste.class);
        classes.add(OmsorgenForRestTjeneste.class);
        classes.add(FordelRestTjeneste.class);
        classes.add(BeregningsresultatRestTjeneste.class);
        classes.add(TotrinnskontrollRestTjeneste.class);
        classes.add(ÅrskvantumRestTjeneste.class);
        classes.add(RammevedtakRestTjeneste.class);
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
        classes.add(SykdomDokumentRestTjeneste.class);
        classes.add(KontrollRestTjeneste.class);
        classes.add(IAYRegisterdataCallbackRestTjeneste.class);
        classes.add(UttakRestTjeneste.class);
        classes.add(PleiepengerUttakRestTjeneste.class);

        // Forvaltningstjenester - fjernes løpende
        classes.add(ProsessTaskRestTjeneste.class);
        classes.add(ForvaltningAksjonspunktSammendragRestTjeneste.class);
        classes.add(ForvaltningMidlertidigDriftRestTjeneste.class);
        classes.add(ForvaltningOppdragRestTjeneste.class);
        classes.add(ForvaltningBeregningRestTjeneste.class);

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
