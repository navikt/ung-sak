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
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.domene.opptjening.Opptjeningsfeil;
import no.nav.foreldrepenger.domene.opptjening.dto.AvklarOpptjeningsvilkåretDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarOpptjeningsvilkåretDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOpptjeningsvilkåretOppdaterer implements AksjonspunktOppdaterer<AvklarOpptjeningsvilkåretDto> {


    private OpptjeningRepository opptjeningRepository;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private HistorikkTjenesteAdapter historikkAdapter;


    AvklarOpptjeningsvilkåretOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public AvklarOpptjeningsvilkåretOppdaterer(OpptjeningRepository opptjeningRepository,
                                               BehandlingRepository behandlingRepository,
                                               VilkårResultatRepository vilkårResultatRepository,
                                               BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                               HistorikkTjenesteAdapter historikkAdapter) {

        this.opptjeningRepository = opptjeningRepository;
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat oppdater(AvklarOpptjeningsvilkåretDto dto, AksjonspunktOppdaterParameter param) {
        Utfall nyttUtfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        Vilkårene vilkårene = vilkårResultatRepository.hent(param.getBehandlingId());

        Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());

        final var fom = Tid.TIDENES_BEGYNNELSE; // FIXME (k9) : legge inn faktiske perioder fra dto
        final var tom = Tid.TIDENES_ENDE;
        if (nyttUtfall.equals(Utfall.OPPFYLT)) {
            sjekkOmVilkåretKanSettesTilOppfylt(param.getBehandlingId());
            oppdaterUtfallOgLagre(behandling, vilkårene, nyttUtfall, kontekst.getSkriveLås(), fom, tom);

            return OppdateringResultat.utenOveropp();
        } else {

            oppdaterUtfallOgLagre(behandling, vilkårene, nyttUtfall, kontekst.getSkriveLås(), fom, tom);

            return OppdateringResultat.medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR);
        }
    }

    private void oppdaterUtfallOgLagre(Behandling behandling, Vilkårene vilkårene, Utfall utfallType, BehandlingLås skriveLås, LocalDate fom, LocalDate tom) {
        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        final var vilkårBuilder = builder.hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(fom, tom)
            .medUtfall(utfallType)
            .medAvslagsårsak(!utfallType.equals(Utfall.OPPFYLT) ? Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING : null));
        builder.leggTil(vilkårBuilder);
        Vilkårene resultat = builder.build();

        vilkårResultatRepository.lagre(behandling.getId(), resultat);
        behandlingRepository.lagre(behandling, skriveLås);
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
