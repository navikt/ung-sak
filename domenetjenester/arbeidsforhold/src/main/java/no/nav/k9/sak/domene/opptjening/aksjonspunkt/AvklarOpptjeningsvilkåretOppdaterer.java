package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.opptjening.Opptjeningsfeil;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.opptjening.AvklarOpptjeningsvilkårDto;
import no.nav.k9.sak.kontrakt.opptjening.AvklarOpptjeningsvilkåretDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarOpptjeningsvilkårDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOpptjeningsvilkåretOppdaterer implements AksjonspunktOppdaterer<AvklarOpptjeningsvilkårDto> {

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
    public OppdateringResultat oppdater(AvklarOpptjeningsvilkårDto dto, AksjonspunktOppdaterParameter param) {
        for (AvklarOpptjeningsvilkåretDto avklarOpptjeningsvilkåretDto : dto.getPerioder()) {
            Utfall nyttUtfall = avklarOpptjeningsvilkåretDto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
            Vilkårene vilkårene = vilkårResultatRepository.hent(param.getBehandlingId());

            Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
            lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());
            BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());

            if (nyttUtfall.equals(Utfall.OPPFYLT)) {
                var periode = DatoIntervallEntitet.fraOgMedTilOgMed(avklarOpptjeningsvilkåretDto.getOpptjeningFom(), avklarOpptjeningsvilkåretDto.getOpptjeningTom());
                sjekkOmVilkåretKanSettesTilOppfylt(param.getBehandlingId(), periode);
                oppdaterUtfallOgLagre(behandling, vilkårene, nyttUtfall, kontekst.getSkriveLås(), avklarOpptjeningsvilkåretDto.getOpptjeningFom(), avklarOpptjeningsvilkåretDto.getOpptjeningTom());
            } else {
                oppdaterUtfallOgLagre(behandling, vilkårene, nyttUtfall, kontekst.getSkriveLås(), avklarOpptjeningsvilkåretDto.getOpptjeningFom(), avklarOpptjeningsvilkåretDto.getOpptjeningTom());
            }
        }
        return OppdateringResultat.utenOveropp();
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

    private void sjekkOmVilkåretKanSettesTilOppfylt(Long behandlingId, DatoIntervallEntitet periode) {
        var opptjening = opptjeningRepository.finnOpptjening(behandlingId).flatMap(it -> it.finnOpptjening(periode));
        if (opptjening.isPresent()) {
            long antall = opptjening.get().getOpptjeningAktivitet().stream()
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
