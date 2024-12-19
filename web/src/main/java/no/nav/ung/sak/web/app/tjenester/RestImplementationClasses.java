package no.nav.ung.sak.web.app.tjenester;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import no.nav.k9.prosesstask.rest.ProsessTaskRestTjeneste;
import no.nav.ung.sak.web.app.proxy.oppdrag.OppdragProxyRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.abakus.IAYRegisterdataCallbackRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.aktør.AktørRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.BehandlingBackendRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.BehandlingRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.aksjonspunkt.AksjonspunktRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.aksjonspunkt.ForvaltningAksjonspunktSammendragRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.beregningsresultat.BeregningsresultatRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.beregningsresultat.OverlapendeYtelserRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.historikk.HistorikkRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.kontroll.KontrollRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.personopplysning.ForvaltningPersonRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.personopplysning.PersonRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.søknad.SøknadRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.søknadsfrist.SøknadsfristRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.tilbakekreving.TilbakekrevingRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.vedtak.TotrinnskontrollRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.behandling.vilkår.VilkårRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.brukerdialog.BrukerdialogRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.fordeling.FordelHendelseRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.fordeling.FordelRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.formidling.FormidlingRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.forvaltning.DiagnostikkRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.forvaltning.ForvaltningOppdragRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.forvaltning.rapportering.RapporteringRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.integrasjonstatus.IntegrasjonstatusRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.kodeverk.KodeverkRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.kravperioder.PerioderTilBehandlingMedKildeRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.los.LosRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.microsoftgraph.ForvaltningTestRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.notat.NotatRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.register.RedirectToRegisterRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.saksbehandler.InitielleLinksRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.saksbehandler.NavAnsattRestTjeneste;
import no.nav.ung.sak.web.app.tjenester.saksbehandler.SaksbehandlerRestTjeneste;
import no.nav.ung.sak.web.app.ungdomsytelse.UngdomsytelseRestTjeneste;
import no.nav.ung.sak.web.server.abac.PipRestTjeneste;

public class RestImplementationClasses {

    public Collection<Class<?>> getImplementationClasses() {
        Set<Class<?>> classes = new HashSet<>();

        classes.add(FagsakRestTjeneste.class);
        classes.add(NavAnsattRestTjeneste.class);
        classes.add(SaksbehandlerRestTjeneste.class);
        classes.add(InitielleLinksRestTjeneste.class);
        classes.add(BehandlingRestTjeneste.class);
        classes.add(BehandlingBackendRestTjeneste.class);
        classes.add(AksjonspunktRestTjeneste.class);
        classes.add(DokumentRestTjeneste.class);
        classes.add(FormidlingRestTjeneste.class);
        classes.add(HistorikkRestTjeneste.class);
        classes.add(KodeverkRestTjeneste.class);
        classes.add(SøknadsfristRestTjeneste.class);
        classes.add(FordelRestTjeneste.class);
        classes.add(FordelHendelseRestTjeneste.class);
        classes.add(BeregningsresultatRestTjeneste.class);
        classes.add(TotrinnskontrollRestTjeneste.class);
        classes.add(PerioderTilBehandlingMedKildeRestTjeneste.class);
        classes.add(PersonRestTjeneste.class);
        classes.add(SøknadRestTjeneste.class);
        classes.add(VilkårRestTjeneste.class);
        classes.add(IntegrasjonstatusRestTjeneste.class);
        classes.add(PipRestTjeneste.class);
        classes.add(TilbakekrevingRestTjeneste.class);
        classes.add(AktørRestTjeneste.class);
        classes.add(KontrollRestTjeneste.class);
        classes.add(IAYRegisterdataCallbackRestTjeneste.class);
        classes.add(OverlapendeYtelserRestTjeneste.class);
        classes.add(RedirectToRegisterRestTjeneste.class);
        classes.add(LosRestTjeneste.class);
        classes.add(BrukerdialogRestTjeneste.class);

        classes.add(UngdomsytelseRestTjeneste.class);

        classes.add(OppdragProxyRestTjeneste.class);

        // Forvaltningstjenester - fjernes løpende
        classes.add(ProsessTaskRestTjeneste.class);
        classes.add(ForvaltningAksjonspunktSammendragRestTjeneste.class);
        classes.add(ForvaltningTestRestTjeneste.class);
        classes.add(ForvaltningOppdragRestTjeneste.class);
        classes.add(ForvaltningPersonRestTjeneste.class);
        classes.add(DiagnostikkRestTjeneste.class);
        classes.add(RapporteringRestTjeneste.class);
        classes.add(NotatRestTjeneste.class);

        return Set.copyOf(classes);
    }


}
