package no.nav.k9.sak.ytelse.unntaksbehandling.revurdering;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.k9.sak.behandling.revurdering.UnntaksbehandlingOppretterTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

@FagsakYtelseTypeRef
@BehandlingTypeRef("BT-010")
@ApplicationScoped
public class BehandlingstypeSpesifikkUnntaksbehandlingOppretter implements UnntaksbehandlingOppretterTjeneste {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private RevurderingTjenesteFelles revurderingTjenesteFelles;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    public BehandlingstypeSpesifikkUnntaksbehandlingOppretter() {
        // for CDI proxy
    }

    @Inject
    public BehandlingstypeSpesifikkUnntaksbehandlingOppretter(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                                              RevurderingTjenesteFelles revurderingTjenesteFelles,
                                                              BehandlingRepositoryProvider behandlingRepositoryProvider) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.revurderingTjenesteFelles = revurderingTjenesteFelles;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = behandlingRepositoryProvider.getBeregningsresultatRepository();
    }

    @Override
    public Behandling opprettNyBehandling(Fagsak fagsak, Behandling origBehandling, BehandlingÅrsakType behandlingÅrsak, OrganisasjonsEnhet enhet) {
        Behandling nyBehandling;
        if (origBehandling == null) {
            nyBehandling = opprettFørsteBehandling(fagsak, behandlingÅrsak, enhet);
        } else {
            nyBehandling = opprettNyBehandling(origBehandling, behandlingÅrsak, true, enhet);
            kopierTilkjentYtelse(origBehandling, nyBehandling);
        }

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(nyBehandling);
        behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst,
            List.of(AksjonspunktDefinisjon.OVERSTYRING_AV_K9_VILKÅRET, AksjonspunktDefinisjon.MANUELL_TILKJENT_YTELSE));

        return nyBehandling;
    }

    private void kopierTilkjentYtelse(Behandling origBehandling, Behandling nyBehandling) {
        beregningsresultatRepository.hentBeregningsresultatAggregat(origBehandling.getId())
            .ifPresent(aggregat -> {
                if (aggregat.getBgBeregningsresultat() != null) {
                    beregningsresultatRepository.lagre(nyBehandling, aggregat.getBgBeregningsresultat());
                }
                if (aggregat.getOverstyrtBeregningsresultat() != null) {
                    beregningsresultatRepository.lagre(nyBehandling, aggregat.getOverstyrtBeregningsresultat());
                }
            });
    }

    public Behandling opprettFørsteBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, OrganisasjonsEnhet enhet) {
        var behandlingType = BehandlingType.UNNTAKSBEHANDLING;

        return behandlingskontrollTjeneste.opprettNyBehandling(fagsak, behandlingType, (beh) -> {
            if (!BehandlingÅrsakType.UDEFINERT.equals(behandlingÅrsakType)) {
                BehandlingÅrsak.builder(behandlingÅrsakType).buildFor(beh);
            }
            beh.setBehandlingstidFrist(LocalDate.now().plusWeeks(behandlingType.getBehandlingstidFristUker()));
            beh.setBehandlendeEnhet(enhet);
        }); // NOSONAR
    }


    private Behandling opprettNyBehandling(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, boolean manueltOpprettet, OrganisasjonsEnhet enhet) {
        behandlingskontrollTjeneste.initBehandlingskontroll(origBehandling);

        // Opprett revurderingsbehandling
        Behandling manuellBehandling = revurderingTjenesteFelles.opprettNyBehandling(BehandlingType.UNNTAKSBEHANDLING, revurderingsÅrsak, origBehandling, manueltOpprettet, enhet);
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(manuellBehandling);
        behandlingskontrollTjeneste.opprettBehandling(kontekst, manuellBehandling);

        return manuellBehandling;
    }

    @Override
    public Boolean kanNyBehandlingOpprettes(Fagsak fagsak) {
        return behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).isEmpty();
    }

}
