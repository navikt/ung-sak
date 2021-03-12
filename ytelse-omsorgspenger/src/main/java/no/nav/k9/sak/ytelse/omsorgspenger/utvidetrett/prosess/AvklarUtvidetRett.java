package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.time.LocalDate;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
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
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.omsorgspenger.AvklarUtvidetRettDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarUtvidetRettDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarUtvidetRett implements AksjonspunktOppdaterer<AvklarUtvidetRettDto> {

    private final VilkårType vilkårType = VilkårType.UTVIDETRETT;
    private final Avslagsårsak defaultAvslagsårsak = Avslagsårsak.IKKE_UTVIDETRETT;
    private final SkjermlenkeType skjermlenkeType = SkjermlenkeType.PUNKT_FOR_UTVIDETRETT;
    private final HistorikkEndretFeltType historikkEndretFeltType = HistorikkEndretFeltType.UTVIDETRETT;

    private HistorikkTjenesteAdapter historikkAdapter;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    AvklarUtvidetRett() {
        // for CDI proxy
    }

    @Inject
    AvklarUtvidetRett(HistorikkTjenesteAdapter historikkAdapter, VilkårResultatRepository vilkårResultatRepository, BehandlingRepository behandlingRepository) {
        this.historikkAdapter = historikkAdapter;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvklarUtvidetRettDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var fagsak = behandling.getFagsak();

        Utfall nyttUtfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        var vilkårBuilder = param.getVilkårResultatBuilder();

        lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());

        var periode = dto.getPeriode();
        if (periode == null) {
            // overskriver hele
            var vilkårene = vilkårResultatRepository.hent(behandlingId);
            var timeline = vilkårene.getVilkårTimeline(vilkårType);
            oppdaterUtfallOgLagre(vilkårBuilder, nyttUtfall, timeline.getMinLocalDate(), timeline.getMaxLocalDate(), dto.getAvslagsårsak());
        } else {
            var angittPeriode = validerAngittPeriode(fagsak, new LocalDateInterval(periode.getFom(), periode.getTom()));
            oppdaterUtfallOgLagre(vilkårBuilder, nyttUtfall, angittPeriode.getFomDato(), angittPeriode.getTomDato(), dto.getAvslagsårsak());
        }
        return OppdateringResultat.utenOveropp();
    }

    private LocalDateInterval validerAngittPeriode(Fagsak fagsak, LocalDateInterval angittPeriode) {
        Objects.requireNonNull(angittPeriode);
        if (FagsakYtelseType.OMSORGSPENGER_KS == fagsak.getYtelseType()) {
            throw new UnsupportedOperationException("Kan ikke angi periode for ytelseType=" + fagsak.getYtelseType());
        }
        var fagsakPeriode = new LocalDateInterval(fagsak.getPeriode().getFomDato(), fagsak.getPeriode().getTomDato());
        if (!fagsakPeriode.contains(angittPeriode)) {
            throw new IllegalArgumentException("Angitt periode må være i det minste innenfor fagsakens periode. angitt=" + angittPeriode + ", fagsakPeriode=" + fagsakPeriode);
        }
        return angittPeriode;

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
