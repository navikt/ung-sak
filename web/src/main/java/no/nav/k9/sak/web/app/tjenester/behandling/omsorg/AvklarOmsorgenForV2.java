package no.nav.k9.sak.web.app.tjenester.behandling.omsorg;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.inngangsvilkår.omsorg.repo.OmsorgenForGrunnlagRepository;
import no.nav.k9.sak.inngangsvilkår.omsorg.repo.OmsorgenForSaksbehandlervurdering;
import no.nav.k9.sak.kontrakt.omsorg.AvklarOmsorgenForDto;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.Fosterbarn;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.FosterbarnRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.Fosterbarna;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarOmsorgenForDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarOmsorgenForV2 implements AksjonspunktOppdaterer<AvklarOmsorgenForDto> {

    private final SkjermlenkeType skjermlenkeType = SkjermlenkeType.FAKTA_OM_OMSORGENFOR;

    private HistorikkTjenesteAdapter historikkAdapter;
    private OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository;

    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;

    private FosterbarnRepository fosterbarnRepository;

    private PersoninfoAdapter personinfoAdapter;

    AvklarOmsorgenForV2() {
        // for CDI proxy
    }

    @Inject
    AvklarOmsorgenForV2(HistorikkTjenesteAdapter historikkAdapter, OmsorgenForGrunnlagRepository omsorgenForGrunnlagRepository,
                        @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                        FosterbarnRepository fosterbarnRepository, PersoninfoAdapter personinfoAdapter) {
        this.historikkAdapter = historikkAdapter;
        this.omsorgenForGrunnlagRepository = omsorgenForGrunnlagRepository;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.fosterbarnRepository = fosterbarnRepository;
        this.personinfoAdapter = personinfoAdapter;
    }


    @Override
    public OppdateringResultat oppdater(AvklarOmsorgenForDto dto, AksjonspunktOppdaterParameter param) {
        Long behandlingId = param.getBehandlingId();

        // Oppretter fosterbarn kun dersom eksplisitt angitt av GUI for omsorgspenger
        if (dto.getFosterbarnForOmsorgspenger() != null) leggTilFosterbarnForOmsorgspenger(dto, param);

        sjekkAtPerioderTilOppdateringErTillatt(dto, param, behandlingId);

        // TODO Omsorg: Løkke over endringene slik at vi får bedre historikk=
        lagHistorikkInnslag(param, "Omsorg manuelt behandlet.");

        final List<OmsorgenForSaksbehandlervurdering> nyeVurderinger = toOmsorgenForSaksbehandlervurderinger(dto);
        omsorgenForGrunnlagRepository.lagreNyeVurderinger(behandlingId, nyeVurderinger);

        return OppdateringResultat.nyttResultat();
    }

    private void sjekkAtPerioderTilOppdateringErTillatt(AvklarOmsorgenForDto dto, AksjonspunktOppdaterParameter param, Long behandlingId) {
        VilkårsPerioderTilVurderingTjeneste vilkårsperioderTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, param.getRef().getFagsakYtelseType(), param.getRef().getBehandlingType());
        LocalDateTimeline<Boolean> tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(vilkårsperioderTjeneste.utled(behandlingId, VilkårType.OMSORGEN_FOR));
        LocalDateTimeline<Boolean> tidslinjeTilOppdatering = TidslinjeUtil.tilTidslinjeKomprimert(dto.getOmsorgsperioder().stream().map(p -> new Periode(p.getPeriode().getFom(), p.getPeriode().getTom())).collect(Collectors.toList()));

        LocalDateTimeline<Boolean> oppdateringUtenforSøknadsperiode = TidslinjeUtil.kunPerioderSomIkkeFinnesI(tidslinjeTilOppdatering, tidslinjeTilVurdering);
        if (!oppdateringUtenforSøknadsperiode.isEmpty()) {
            throw new IllegalArgumentException("Oppdatering av omsorgen for utenfor søknadsperiode er ikke tillatt");
        }
    }

    private void leggTilFosterbarnForOmsorgspenger(AvklarOmsorgenForDto dto, AksjonspunktOppdaterParameter param) {
        var fosterbarn = dto.getFosterbarnForOmsorgspenger().stream()
            .map(barn -> personinfoAdapter.hentAktørIdForPersonIdent(new PersonIdent(barn.getFnr())).orElseThrow(() -> new IllegalArgumentException("Finner ikke fnr")))
            .map(aktørId -> new Fosterbarn(aktørId))
            .collect(Collectors.toSet());
        fosterbarnRepository.lagreOgFlush(param.getBehandlingId(), new Fosterbarna(fosterbarn));
    }

    private List<OmsorgenForSaksbehandlervurdering> toOmsorgenForSaksbehandlervurderinger(AvklarOmsorgenForDto dto) {
        return dto.getOmsorgsperioder()
            .stream()
            .map(op -> new OmsorgenForSaksbehandlervurdering(
                DatoIntervallEntitet.fraOgMedTilOgMed(op.getPeriode().getFom(), op.getPeriode().getTom()),
                op.getBegrunnelse(),
                op.getResultat(),
                getCurrentUserId(),
                LocalDateTime.now()
            ))
            .collect(Collectors.toList());
    }

    private static String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }

    private void lagHistorikkInnslag(AksjonspunktOppdaterParameter param, String begrunnelse) {
        boolean erBegrunnelseForAksjonspunktEndret = param.erBegrunnelseEndret();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, erBegrunnelseForAksjonspunktEndret)
            .medSkjermlenke(skjermlenkeType);
    }
}
