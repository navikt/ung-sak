package no.nav.k9.sak.ytelse.unntaksbehandling.mottak;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandling.revurdering.GrunnlagKopierer;
import no.nav.k9.sak.behandling.revurdering.RevurderingTjenesteFelles;
import no.nav.k9.sak.behandling.revurdering.UnntaksbehandlingOppretter;
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
public class UnntaksbehandlingOppretterTjeneste implements UnntaksbehandlingOppretter {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private RevurderingTjenesteFelles revurderingTjenesteFelles;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private Instance<GrunnlagKopierer> grunnlagKopierere;

    public UnntaksbehandlingOppretterTjeneste() {
        // for CDI proxy
    }

    @Inject
    public UnntaksbehandlingOppretterTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                              RevurderingTjenesteFelles revurderingTjenesteFelles,
                                              BehandlingRepositoryProvider behandlingRepositoryProvider,
                                              @Any Instance<GrunnlagKopierer> grunnlagKopierere) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.revurderingTjenesteFelles = revurderingTjenesteFelles;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = behandlingRepositoryProvider.getBeregningsresultatRepository();
        this.grunnlagKopierere = grunnlagKopierere;
    }

    @Override
    public Behandling opprettNyBehandling(Fagsak fagsak, Behandling origBehandling, BehandlingÅrsakType behandlingÅrsak, OrganisasjonsEnhet enhet) {
        validerTilstand(origBehandling);

        Behandling nyBehandling;
        if (origBehandling == null) {
            nyBehandling = opprettFørsteBehandling(fagsak, behandlingÅrsak, enhet);
        } else {
            nyBehandling = opprettNyBehandling(origBehandling, behandlingÅrsak, true, enhet);
            kopierGrunnlag(origBehandling, nyBehandling);
            kopierTilkjentYtelse(origBehandling, nyBehandling);
        }

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(nyBehandling);
        behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, List.of(AksjonspunktDefinisjon.OVERSTYRING_AV_K9_VILKÅRET));

        return nyBehandling;
    }

    private void kopierGrunnlag(Behandling origBehandling, Behandling nyBehandling) {
        var grunnlagKopierer = getGrunnlagKopierer(origBehandling.getFagsakYtelseType());
        grunnlagKopierer.kopierGrunnlagVedManuellOpprettelse(origBehandling, nyBehandling);
    }

    private void kopierTilkjentYtelse(Behandling origBehandling, Behandling nyBehandling) {
        beregningsresultatRepository.hentBeregningsresultatAggregat(origBehandling.getId())
            .ifPresent(aggregat -> {
                // Initiere overstyrt tilkjent ytelse som forrige tilkjente ytelse
                if (aggregat.getUtbetBeregningsresultat() != null) {
                    beregningsresultatRepository.lagre(nyBehandling, aggregat.getUtbetBeregningsresultat());
                } else if (aggregat.getBgBeregningsresultat() != null) {
                    beregningsresultatRepository.lagre(nyBehandling, aggregat.getBgBeregningsresultat());
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

    private GrunnlagKopierer getGrunnlagKopierer(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(grunnlagKopierere, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Kopiering av grunnlag for unntaksbehandling ikke støttet for " + ytelseType.getKode()));
    }

    private void validerTilstand(Behandling origBehandling) {
        if (origBehandling != null && !kanNyBehandlingOpprettes(origBehandling.getFagsak())) {
            throw new IllegalStateException("Kan ikke opprette unntaksbehandling på fagsak");
        }
    }

}
