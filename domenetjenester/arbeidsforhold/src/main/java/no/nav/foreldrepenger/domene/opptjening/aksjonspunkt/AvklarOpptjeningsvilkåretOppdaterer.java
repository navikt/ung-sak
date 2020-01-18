package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Utfall;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.domene.opptjening.Opptjeningsfeil;
import no.nav.foreldrepenger.domene.opptjening.dto.AvklarOpptjeningsvilkåretDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarOpptjeningsvilkåretDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOpptjeningsvilkåretOppdaterer implements AksjonspunktOppdaterer<AvklarOpptjeningsvilkåretDto> {


    private OpptjeningRepository opptjeningRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private HistorikkTjenesteAdapter historikkAdapter;


    AvklarOpptjeningsvilkåretOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarOpptjeningsvilkåretOppdaterer(OpptjeningRepository opptjeningRepository,
                                               BehandlingRepository behandlingRepository,
                                               BehandlingsresultatRepository behandlingsresultatRepository,
                                               BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                               HistorikkTjenesteAdapter historikkAdapter) {

        this.opptjeningRepository = opptjeningRepository;
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat oppdater(AvklarOpptjeningsvilkåretDto dto, AksjonspunktOppdaterParameter param) {
        Utfall nyttUtfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        VilkårResultat vilkårResultat = behandlingsresultatRepository.hent(param.getBehandlingId()).getVilkårResultat();

        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());

        if (nyttUtfall.equals(Utfall.OPPFYLT)) {
            sjekkOmVilkåretKanSettesTilOppfylt(param.getBehandlingId());
            oppdaterUtfallOgLagre(behandling, vilkårResultat, nyttUtfall);
            return OppdateringResultat.utenOveropp();
        }

        oppdaterUtfallOgLagre(behandling, vilkårResultat, nyttUtfall);

        return OppdateringResultat.medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR);
    }

    private void oppdaterUtfallOgLagre(Behandling behandling, VilkårResultat vilkårResultat, Utfall utfallType) {
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());
        VilkårResultatBuilder builder = VilkårResultat.builderFraEksisterende(vilkårResultat);
        final var vilkårBuilder = builder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET);
        // FIXME (k9) : legge inn faktiske perioder fra dto
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)
            .medUtfall(utfallType)
            .medAvslagsårsak(!utfallType.equals(Utfall.OPPFYLT) ? Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING : null));
        builder.leggTil(vilkårBuilder);
        VilkårResultat resultat = builder.build();
        behandling.getBehandlingsresultat().medOppdatertVilkårResultat(resultat);
        behandlingRepository.lagre(resultat, kontekst.getSkriveLås());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private void sjekkOmVilkåretKanSettesTilOppfylt(Long behandlingId) {
        final Optional<Opptjening> opptjening = opptjeningRepository.finnOpptjening(behandlingId);
        if (opptjening.isPresent()) {
            final long antall = opptjening.get().getOpptjeningAktivitet().stream()
                .filter(oa -> !oa.getAktivitetType().equals(OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD)).count();
            if (antall > 0) {
                return;
            }
        }
        throw Opptjeningsfeil.FACTORY.opptjeningPreconditionFailed().toException();
    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, Utfall nyVerdi, String begrunnelse) {
        historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.OPPTJENINGSVILKARET, null, nyVerdi);

        boolean erBegrunnelseForAksjonspunktEndret = param.erBegrunnelseEndret();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, erBegrunnelseForAksjonspunktEndret)
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_OPPTJENING);
    }
}
