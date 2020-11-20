package no.nav.k9.sak.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.inngangsvilkår.InngangsvilkårTjeneste;
import no.nav.k9.sak.kontrakt.vilkår.Overstyringk9VilkåretDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = Overstyringk9VilkåretDto.class, adapter = Overstyringshåndterer.class)
public class K9VilkåretOverstyringshåndterer extends AbstractOverstyringshåndterer<Overstyringk9VilkåretDto> {

    private static final Set<BehandlingResultatType> UNNTAKSBEHANDLING_KODER = Set.of(
        BehandlingResultatType.INNVILGET,
        BehandlingResultatType.AVSLÅTT);

    private InngangsvilkårTjeneste inngangsvilkårTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;

    K9VilkåretOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public K9VilkåretOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                           InngangsvilkårTjeneste inngangsvilkårTjeneste,
                                           BeregningsresultatRepository beregningsresultatRepository) {
        super(historikkAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_K9_VILKÅRET);
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    @Override
    public OppdateringResultat håndterOverstyring(Overstyringk9VilkåretDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        var behandlingResultatType = dto.getBehandlingResultatType();
        validerBehandlingsresultat(behandlingResultatType);
        behandling.setBehandlingResultatType(behandlingResultatType);
        if (BehandlingResultatType.AVSLÅTT.equals(behandlingResultatType)) {
            nullstillTilkjentYtelse(behandling, kontekst);
        }

        var utfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        var periode = dto.getPeriode();
        inngangsvilkårTjeneste.overstyrAksjonspunkt(behandling.getId(), VilkårType.K9_VILKÅRET, utfall, dto.getAvslagskode(),
            kontekst, periode.getFom(), periode.getTom(), dto.getBegrunnelse());

        return OppdateringResultat.utenOveropp();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, Overstyringk9VilkåretDto dto) {
        lagHistorikkInnslagForOverstyrtVilkår(dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_MAN_VILKÅRSVURDERING);
    }


    private void validerBehandlingsresultat(BehandlingResultatType behandlingResultatType) {
        if (!UNNTAKSBEHANDLING_KODER.contains(behandlingResultatType)) {
            throw new IllegalArgumentException("Ugyldig behandlingsresultattype " + behandlingResultatType.getKode());
        }
    }

    private void nullstillTilkjentYtelse(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        var origBehandlingId = behandling.getOriginalBehandlingId();

        if (!origBehandlingId.isPresent()) {
            beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), kontekst.getSkriveLås());
            return;
        }

        // Reverter til beregningsresultat fra forrige behandling
        beregningsresultatRepository.hentBeregningsresultatAggregat(origBehandlingId.get())
            .ifPresent(origAggregat -> {
                if (origAggregat.getBgBeregningsresultat() != null) {
                    beregningsresultatRepository.lagre(behandling, origAggregat.getBgBeregningsresultat());
                }
                if (origAggregat.getOverstyrtBeregningsresultat() != null) {
                    beregningsresultatRepository.lagre(behandling, origAggregat.getOverstyrtBeregningsresultat());
                }
            });
    }
}
