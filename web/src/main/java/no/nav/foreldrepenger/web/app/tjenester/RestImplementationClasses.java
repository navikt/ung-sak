package no.nav.foreldrepenger.web.app.tjenester;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import no.nav.foreldrepenger.web.app.tjenester.abakus.IAYRegisterdataCallbackRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.aktør.AktørRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingBackendRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.BehandlingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold.InntektArbeidYtelseRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.BeregningsgrunnlagRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat.BeregningsresultatRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.historikk.HistorikkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.KontrollRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.opptjening.OpptjeningRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.TotrinnskontrollRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.VilkårRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.brev.BrevRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.FordelRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forsendelse.ForsendelseStatusRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.integrasjonstatus.IntegrasjonstatusRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.KodeverkRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.konfig.KonfigRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.saksbehandler.FeatureToggleRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.saksbehandler.NavAnsattRestTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.vedtak.VedtakRestTjeneste;
import no.nav.foreldrepenger.web.server.abac.PipRestTjeneste;
import no.nav.vedtak.felles.prosesstask.rest.ProsessTaskRestTjeneste;

public class RestImplementationClasses {

    public Collection<Class<?>> getImplementationClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(FagsakRestTjeneste.class);
        classes.add(NavAnsattRestTjeneste.class);
        classes.add(FeatureToggleRestTjeneste.class);
        classes.add(BehandlingRestTjeneste.class);
        classes.add(BehandlingBackendRestTjeneste.class);
        classes.add(BeregningsgrunnlagRestTjeneste.class);
        classes.add(AksjonspunktRestTjeneste.class);
        classes.add(DokumentRestTjeneste.class);
        classes.add(HistorikkRestTjeneste.class);
        classes.add(KodeverkRestTjeneste.class);
        classes.add(KonfigRestTjeneste.class);
        classes.add(ProsessTaskRestTjeneste.class);
        classes.add(FordelRestTjeneste.class);
        classes.add(BeregningsresultatRestTjeneste.class);
        classes.add(VedtakRestTjeneste.class);
        classes.add(TotrinnskontrollRestTjeneste.class);
        classes.add(ForsendelseStatusRestTjeneste.class);
        classes.add(PersonRestTjeneste.class);
        classes.add(SøknadRestTjeneste.class);
        classes.add(OpptjeningRestTjeneste.class);
        classes.add(InntektArbeidYtelseRestTjeneste.class);
        classes.add(VilkårRestTjeneste.class);
        classes.add(IntegrasjonstatusRestTjeneste.class);
        classes.add(PipRestTjeneste.class);
        classes.add(TilbakekrevingRestTjeneste.class);
        classes.add(AktørRestTjeneste.class);
        classes.add(KontrollRestTjeneste.class);
        classes.add(IAYRegisterdataCallbackRestTjeneste.class);

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
