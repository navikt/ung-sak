package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess;

import java.time.LocalDate;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
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
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.omsorgspenger.AvklarUtvidetRettDto;
import no.nav.k9.sak.typer.Periode;

/**
 * @deprecated skal erstattes av AvklarUtvidetRettV2 når kronisk syk/midlertidig alene ny periode håntering er uttestet.
 */
@Deprecated(forRemoval = true)
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
    private SøknadRepository søknadRepository;

    // TODO:fjern flagg når avklart at ny håndtering fungerer også godt for Kronisk syk/Midlertidig Alene
    private boolean enableNyAvklarUtvidetRett;

    AvklarUtvidetRett() {
        // for CDI proxy
    }

    @Inject
    AvklarUtvidetRett(HistorikkTjenesteAdapter historikkAdapter,
                      VilkårResultatRepository vilkårResultatRepository,
                      SøknadRepository søknadRepository,
                      BehandlingRepository behandlingRepository,
                      @KonfigVerdi(value = "ENABLE_NY_AVKLAR_UTVIDET_RETT", defaultVerdi = "true") boolean enableNyAvklarUtvidetRett) {
        this.historikkAdapter = historikkAdapter;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.søknadRepository = søknadRepository;
        this.behandlingRepository = behandlingRepository;
        this.enableNyAvklarUtvidetRett = enableNyAvklarUtvidetRett;
    }

    @Override
    public OppdateringResultat oppdater(AvklarUtvidetRettDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var fagsak = behandling.getFagsak();

        if (enableNyAvklarUtvidetRett
            || fagsak.getYtelseType() == FagsakYtelseType.OMSORGSPENGER_AO) {
            // ny håndtering - sletter hele periodene først (tar først på OMS_AO venter med andre rammevedtak)
            var avklarV2 = new AvklarUtvidetRettV2(historikkAdapter, vilkårResultatRepository, søknadRepository, behandlingRepository);
            return avklarV2.oppdater(dto, param);
        } else {
            // TODO: avvikle denne (bruk over)
            return legacyAvklarUtvidetRett(dto, param, behandlingId, fagsak);
        }
    }

    private OppdateringResultat legacyAvklarUtvidetRett(AvklarUtvidetRettDto dto, AksjonspunktOppdaterParameter param, Long behandlingId, Fagsak fagsak) {
        Utfall nyttUtfall = dto.getErVilkarOk() ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT;
        var vilkårResultatBuilder = param.getVilkårResultatBuilder();

        lagHistorikkInnslag(param, nyttUtfall, dto.getBegrunnelse());

        var periode = dto.getPeriode();
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        var timeline = vilkårene.getVilkårTimeline(vilkårType);
        boolean erAvslag = dto.getAvslagsårsak() != null;
        if (erAvslag || erÅpenPeriode(periode)) {
            // overskriver hele vilkårperioden
            // TODO: bør mulig avgrense fra søknad#mottattdato / søknadsperiode#fom i forbindelse med endringer?
            var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(vilkårType);
            oppdaterUtfallOgLagre(vilkårBuilder, nyttUtfall, timeline.getMinLocalDate(), timeline.getMaxLocalDate(), dto.getAvslagsårsak());
            vilkårResultatBuilder.leggTil(vilkårBuilder);
        } else {
            var søknadsperiode = søknadRepository.hentSøknad(behandlingId).getSøknadsperiode();
            vilkårResultatBuilder.slettVilkårPerioder(vilkårType, DatoIntervallEntitet.fraOgMed(søknadsperiode.getFomDato()));

            var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(vilkårType);
            var angittPeriode = validerAngittPeriode(fagsak, new LocalDateInterval(periode.getFom(), periode.getTom()));
            oppdaterUtfallOgLagre(vilkårBuilder, nyttUtfall, angittPeriode.getFomDato(), angittPeriode.getTomDato(), dto.getAvslagsårsak());
            vilkårResultatBuilder.leggTil(vilkårBuilder);
        }

        return OppdateringResultat.utenOveropp();
    }

    private boolean erÅpenPeriode(Periode periode) {
        return periode == null || !new LocalDateInterval(periode.getFom(), periode.getTom()).isClosedInterval();
    }

    private LocalDateInterval validerAngittPeriode(Fagsak fagsak, LocalDateInterval angittPeriode) {
        if (Objects.requireNonNull(angittPeriode).isOpenStart()) {
            throw new IllegalArgumentException("Angitt periode kan ikke ha åpen start. angitt=" + angittPeriode);
        }

        var fagsakPeriode = fagsak.getPeriode().toLocalDateInterval();
        if (!fagsakPeriode.contains(angittPeriode)) {
            throw new IllegalArgumentException("Angitt periode må være i det minste innenfor fagsakens periode. angitt=" + angittPeriode + ", fagsakPeriode=" + fagsakPeriode);
        }
        return angittPeriode;
    }

    private void oppdaterUtfallOgLagre(VilkårBuilder builder, Utfall utfallType, LocalDate fom, LocalDate tom, Avslagsårsak avslagsårsak) {
        Avslagsårsak settAvslagsårsak = !utfallType.equals(Utfall.OPPFYLT) ? (avslagsårsak == null ? defaultAvslagsårsak : avslagsårsak) : null;
        builder.leggTil(builder.hentBuilderFor(fom, tom)
            .medUtfallManuell(utfallType)
            .medAvslagsårsak(settAvslagsårsak));

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
