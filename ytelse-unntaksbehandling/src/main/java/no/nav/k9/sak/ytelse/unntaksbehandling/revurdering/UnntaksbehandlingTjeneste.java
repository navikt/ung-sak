package no.nav.k9.sak.ytelse.unntaksbehandling.revurdering;

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
import no.nav.k9.sak.behandling.revurdering.NyBehandlingTjeneste;
import no.nav.k9.sak.behandling.revurdering.RevurderingTjenesteFelles;
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
public class UnntaksbehandlingTjeneste implements NyBehandlingTjeneste {

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private RevurderingTjenesteFelles revurderingTjenesteFelles;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private Instance<GrunnlagKopierer> grunnlagKopierere;

    public UnntaksbehandlingTjeneste() {
        // for CDI proxy
    }

    @Inject
    public UnntaksbehandlingTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
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
    public Behandling opprettAutomatiskNyBehandling(Behandling origBehandling, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet) {
        return opprettManueltNyBehandling(origBehandling.getFagsak(), origBehandling, revurderingsÅrsak, enhet);
    }

    @Override
    public Behandling opprettManueltNyBehandling(Fagsak fagsak, Behandling origBehandling, BehandlingÅrsakType behandlingÅrsak, OrganisasjonsEnhet enhet) {
        validerTilstand(origBehandling);

        Behandling nyBehandling;
        if (origBehandling == null) {
            nyBehandling = opprettFørsteBehandling(fagsak, behandlingÅrsak, enhet);
        } else {
            nyBehandling = opprettNyBehandling(origBehandling, behandlingÅrsak, true, enhet);
            kopierAlleGrunnlagFraTidligereBehandling(origBehandling, nyBehandling);
            kopierTilkjentYtelseFraTidligereBehandling(origBehandling, nyBehandling);
        }

        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(nyBehandling);
        behandlingskontrollTjeneste.lagreAksjonspunkterFunnet(kontekst, List.of(AksjonspunktDefinisjon.OVERSTYRING_AV_K9_VILKÅRET));

        return nyBehandling;
    }

    @Override
    public void kopierAlleGrunnlagFraTidligereBehandling(Behandling origBehandling, Behandling nyBehandling) {
        var grunnlagKopierer = getGrunnlagKopierer(origBehandling.getFagsakYtelseType());
        grunnlagKopierer.kopierGrunnlagVedManuellOpprettelse(origBehandling, nyBehandling);
    }

    @Override
    public Boolean kanNyBehandlingOpprettes(Fagsak fagsak) {
        return behandlingRepository.hentÅpneBehandlingerForFagsakId(fagsak.getId()).isEmpty();
    }

    private void validerTilstand(Behandling origBehandling) {
        if (origBehandling != null && !kanNyBehandlingOpprettes(origBehandling.getFagsak())) {
            throw new IllegalStateException("Kan ikke opprette unntaksbehandling på fagsak");
        }
    }

    private void kopierTilkjentYtelseFraTidligereBehandling(Behandling origBehandling, Behandling nyBehandling) {
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

    private Behandling opprettFørsteBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType, OrganisasjonsEnhet enhet) {
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


    private GrunnlagKopierer getGrunnlagKopierer(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(grunnlagKopierere, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Kopiering av grunnlag for unntaksbehandling ikke støttet for " + ytelseType.getKode()));
    }

}
