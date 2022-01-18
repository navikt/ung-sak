package no.nav.k9.sak.web.app.tjenester.behandling.omsorg;

import java.time.LocalDate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.omsorgspenger.AvklarOmsorgenForDto;
import no.nav.k9.sak.typer.Periode;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarOmsorgenForDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOmsorgenFor implements AksjonspunktOppdaterer<AvklarOmsorgenForDto> {

    private final Avslagsårsak defaultAvslagsårsak = Avslagsårsak.IKKE_DOKUMENTERT_OMSORGEN_FOR;
    private final VilkårType vilkårType = VilkårType.OMSORGEN_FOR;
    private final HistorikkEndretFeltType historikkEndretFeltType = HistorikkEndretFeltType.OMSORG_FOR;
    private final SkjermlenkeType skjermlenkeType = SkjermlenkeType.FAKTA_OM_OMSORGENFOR;

    private HistorikkTjenesteAdapter historikkAdapter;
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;
    private PersoninfoAdapter personinfoAdapter;

    AvklarOmsorgenFor() {
        // for CDI proxy
    }

    @Inject
    AvklarOmsorgenFor(VilkårResultatRepository vilkårResultatRepository,
                      BehandlingRepository behandlingRepository,
                      PersoninfoAdapter personinfoAdapter,
                      HistorikkTjenesteAdapter historikkAdapter) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat oppdater(AvklarOmsorgenForDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var fagsak = behandling.getFagsak();

        Long behandlingId = param.getBehandlingId();

        // skal ha fra før
        var periode = dto.getPeriode();

        Utfall nyttUtfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        var vilkårBuilder = param.getVilkårResultatBuilder();

        lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());

        boolean erAvslag = dto.getAvslagsårsak() != null;
        if (erAvslag || erÅpenPeriode(periode)) {
            // overskriver hele
            var vilkårene = vilkårResultatRepository.hent(behandlingId);
            var timeline = vilkårene.getVilkårTimeline(vilkårType);
            oppdaterUtfallOgLagre(vilkårBuilder, nyttUtfall, timeline.getMinLocalDate(), timeline.getMaxLocalDate(), dto.getAvslagsårsak());
        } else {
            var fagsakPeriode = new LocalDateInterval(fagsak.getPeriode().getFomDato(), fagsak.getPeriode().getTomDato());
            var angittPeriode = new LocalDateInterval(periode.getFom(), periode.getTom());
            if (!fagsakPeriode.contains(angittPeriode)) {
                var barninfo = personinfoAdapter.hentBrukerBasisForAktør(fagsak.getPleietrengendeAktørId()).orElseThrow(() -> new IllegalStateException("Mangler person info for barn"));
                if (angittPeriode.getFomDato().isBefore(barninfo.getFødselsdato())) {
                    throw new IllegalArgumentException("Kan ikke sette angitt periode for omsorg før fødseldato barn: " + barninfo.getFødselsdato());
                } else {
                    throw new IllegalArgumentException("Angitt periode må være i det minste innenfor fagsakens periode. angitt=" + angittPeriode + ", fagsakPeriode=" + fagsakPeriode);
                }
            }
            oppdaterUtfallOgLagre(vilkårBuilder, nyttUtfall, angittPeriode.getFomDato(), angittPeriode.getTomDato(), dto.getAvslagsårsak());
        }

        return OppdateringResultat.utenOverhopp();
    }

    private boolean erÅpenPeriode(Periode periode) {
        return periode == null || !new LocalDateInterval(periode.getFom(), periode.getTom()).isClosedInterval();
    }

    private void oppdaterUtfallOgLagre(VilkårResultatBuilder builder, Utfall utfallType, LocalDate fom, LocalDate tom, Avslagsårsak avslagsårsak) {
        var vilkårBuilder = builder.hentBuilderFor(vilkårType);
        Avslagsårsak settAvslagsårsak = !utfallType.equals(Utfall.OPPFYLT) ? (avslagsårsak == null ? defaultAvslagsårsak : avslagsårsak) : null;
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(fom, tom)
            .medUtfallManuell(utfallType)
            .medAvslagsårsak(settAvslagsårsak));
        builder.leggTil(vilkårBuilder);
    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, Utfall nyVerdi, String begrunnelse) {
        historikkAdapter.tekstBuilder()
            .medEndretFelt(historikkEndretFeltType, null, nyVerdi);

        boolean erBegrunnelseForAksjonspunktEndret = param.erBegrunnelseEndret();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, erBegrunnelseForAksjonspunktEndret)
            .medSkjermlenke(skjermlenkeType);
    }
}
