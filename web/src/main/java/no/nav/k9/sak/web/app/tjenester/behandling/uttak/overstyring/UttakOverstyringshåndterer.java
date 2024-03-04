package no.nav.k9.sak.web.app.tjenester.behandling.uttak.overstyring;

import static no.nav.k9.kodeverk.historikk.HistorikkinnslagType.OVST_UTTAK_FJERNET;
import static no.nav.k9.kodeverk.historikk.HistorikkinnslagType.OVST_UTTAK_NY;
import static no.nav.k9.kodeverk.historikk.HistorikkinnslagType.OVST_UTTAK_OPPDATERT;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrUttakRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrtUttakPeriode;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrtUttakUtbetalingsgrad;
import no.nav.k9.sak.domene.arbeidsforhold.aksjonspunkt.ArbeidsgiverHistorikkinnslag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakPeriodeDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakSlettPeriodeDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakUtbetalingsgradDto;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrUttakDto.class, adapter = Overstyringshåndterer.class)
public class UttakOverstyringshåndterer implements Overstyringshåndterer<OverstyrUttakDto> {

    private OverstyrUttakRepository overstyrUttakRepository;
    private HistorikkRepository historikkRepository;
    private ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester;

    UttakOverstyringshåndterer() {
        //for CDI proxy
    }

    @Inject
    public UttakOverstyringshåndterer(OverstyrUttakRepository overstyrUttakRepository, HistorikkRepository historikkRepository, ArbeidsgiverHistorikkinnslag arbeidsgiverHistorikkinnslag,
                                      @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjenester) {
        this.overstyrUttakRepository = overstyrUttakRepository;
        this.historikkRepository = historikkRepository;
        this.arbeidsgiverHistorikkinnslag = arbeidsgiverHistorikkinnslag;
        this.perioderTilVurderingTjenester = perioderTilVurderingTjenester;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrUttakDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        if (!behandling.getFagsakYtelseType().equals(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)) {
            throw new IllegalArgumentException("Overstyring av uttak er kun aktivert for PSB");
        }
        LocalDateTimeline<OverstyrtUttakPeriode> overstyringerFørOppdatering = overstyrUttakRepository.hentOverstyrtUttak(behandling.getId());

        LocalDateTimeline<OverstyrtUttakPeriode> oppdateringer = new LocalDateTimeline<>(dto.getLagreEllerOppdater().stream().map(this::mapTilSegment).toList());
        List<Long> perioderTilSletting = dto.getSlett().stream().map(OverstyrUttakSlettPeriodeDto::getId).toList();
        overstyrUttakRepository.oppdaterOverstyringAvUttak(behandling.getId(), perioderTilSletting, oppdateringer);

        LocalDateTimeline<OverstyrtUttakPeriode> overstyringerEtterOppdatering = overstyrUttakRepository.hentOverstyrtUttak(behandling.getId());

        validerGyldigEndring(behandling, overstyringerFørOppdatering, overstyringerEtterOppdatering);

        if (dto.gåVidere()) {
            return OppdateringResultat.builder().build();
        } else {
            return OppdateringResultat.builder().medEkstraAksjonspunktResultat(AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAK, AksjonspunktStatus.OPPRETTET).build();
        }
    }

    private void validerGyldigEndring(Behandling behandling, LocalDateTimeline<OverstyrtUttakPeriode> overstyringerFørOppdatering, LocalDateTimeline<OverstyrtUttakPeriode> overstyringerEtterOppdatering) {
        var perioderTilVurdering = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(perioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType()).utledFraDefinerendeVilkår(behandling.getId());
        var tilVurderingTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering);
        var harEndringUtenforGyldigPeriode = !overstyringerFørOppdatering.crossJoin(overstyringerEtterOppdatering, (di, lhs, rhs) -> new LocalDateSegment<>(di, !Objects.equals(lhs, rhs)))
            .filterValue(it -> it)
            .disjoint(tilVurderingTidslinje)
            .isEmpty();

        if (harEndringUtenforGyldigPeriode) {
            throw new IllegalArgumentException("Kan ikke endre på overstyring utenfor periode til vurdering");
        }
    }

    @Override
    public void håndterAksjonspunktForOverstyringPrecondition(OverstyrUttakDto dto, Behandling behandling) {

    }

    @Override
    public void håndterAksjonspunktForOverstyringHistorikk(OverstyrUttakDto dto, Behandling behandling, boolean endretBegrunnelse) {
        //håndtert i håndterOverstyring-metoden
    }

    @Override
    public AksjonspunktDefinisjon aksjonspunktForInstans() {
        return AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAK;
    }

    private void lagreHistorikkinnslagForEndringer(Long behandingId, LocalDateTimeline<OverstyrtUttakPeriode> overstyringerFørOppdatering, LocalDateTimeline<OverstyrtUttakPeriode> overstyringerEtterOppdatering) {
        lagreHistorikkinnslag(behandingId, overstyringerEtterOppdatering, overstyringerFørOppdatering);
    }

    private void lagreHistorikkinnslag(Long behandlingId, LocalDateTimeline<OverstyrtUttakPeriode> overstyringerEtterOppdatering, LocalDateTimeline<OverstyrtUttakPeriode> overstyringerFørOppdatering) {
        overstyringerEtterOppdatering.crossJoin(overstyringerFørOppdatering, this::lagInnslag).forEach(segment -> {
            segment.getValue().setBehandlingId(behandlingId);
            historikkRepository.lagre(segment.getValue());
        });
    }

    private LocalDateSegment<Historikkinnslag> lagInnslag(LocalDateInterval di, LocalDateSegment<OverstyrtUttakPeriode> etterOppdatering, LocalDateSegment<OverstyrtUttakPeriode> førOppdatering) {
        Historikkinnslag innslag = new Historikkinnslag();
        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder().medSkjermlenke(SkjermlenkeType.UTTAK);

        if (etterOppdatering == null) {
            innslag.setType(OVST_UTTAK_FJERNET);
            tekstBuilder.medTema(HistorikkEndretFeltType.OVST_UTTAK_FJERNET, formater(di));
            tekstBuilder.medHendelse(OVST_UTTAK_FJERNET);
        } else if (førOppdatering == null) {
            innslag.setType(OVST_UTTAK_NY);
            tekstBuilder.medHendelse(OVST_UTTAK_NY);
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.UTTAK_OVERSTYRT_PERIODE, null, formater(di));
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.UTTAK_OVERSTYRT_SØKERS_UTTAKSGRAD, null, etterOppdatering.getValue().getSøkersUttaksgrad());
            leggTilUtbetalingsgradEndredeFelter(etterOppdatering, tekstBuilder);
            tekstBuilder.medBegrunnelse(etterOppdatering.getValue().getBegrunnelse());
        } else {
            innslag.setType(OVST_UTTAK_OPPDATERT);
            tekstBuilder.medHendelse(OVST_UTTAK_OPPDATERT);
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.UTTAK_OVERSTYRT_PERIODE, null, formater(di));
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.UTTAK_OVERSTYRT_SØKERS_UTTAKSGRAD, førOppdatering.getValue().getSøkersUttaksgrad(), etterOppdatering.getValue().getSøkersUttaksgrad());
            leggTilUtbetalingsgradEndredeFelter(etterOppdatering, tekstBuilder);
            tekstBuilder.medBegrunnelse(etterOppdatering.getValue().getBegrunnelse());
        }

        tekstBuilder.build(innslag);

        return new LocalDateSegment<>(di, innslag);
    }

    private void leggTilUtbetalingsgradEndredeFelter(LocalDateSegment<OverstyrtUttakPeriode> etterOppdatering, HistorikkInnslagTekstBuilder tekstBuilder) {
        etterOppdatering.getValue().getOverstyrtUtbetalingsgrad().forEach(o -> {
            if (o.getArbeidsgiver() != null) {
                var arbeidsgiverTekst = arbeidsgiverHistorikkinnslag.lagArbeidsgiverHistorikkinnslagTekst(o.getArbeidsgiver(), o.getInternArbeidsforholdRef(), List.of());
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.UTTAK_OVERSTYRT_UTBETALINGSGRAD,
                    arbeidsgiverTekst,
                    null, o.getUtbetalingsgrad());
            } else {
                tekstBuilder.medEndretFelt(HistorikkEndretFeltType.UTTAK_OVERSTYRT_UTBETALINGSGRAD,
                    o.getAktivitetType().getNavn(),
                    null, o.getUtbetalingsgrad());
            }
        });
    }

    private LocalDateSegment<OverstyrtUttakPeriode> mapTilSegment(OverstyrUttakPeriodeDto periodeDto) {
        return new LocalDateSegment<>(periodeDto.getPeriode().getFom(), periodeDto.getPeriode().getTom(), map(periodeDto));
    }


    private OverstyrtUttakPeriode map(OverstyrUttakPeriodeDto dto) {
        Set<OverstyrtUttakUtbetalingsgrad> utbetalingsgrader = dto.getUtbetalingsgrader().stream().map(this::map).collect(Collectors.toSet());
        return new OverstyrtUttakPeriode(dto.getId(), dto.getSøkersUttaksgrad(), utbetalingsgrader, dto.getBegrunnelse());
    }

    private OverstyrtUttakUtbetalingsgrad map(OverstyrUttakUtbetalingsgradDto dto) {
        OverstyrUttakArbeidsforholdDto arbeidsforhold = dto.getArbeidsforhold();
        String orgnr = arbeidsforhold.getOrgnr() != null ? arbeidsforhold.getOrgnr().getOrgNummer() : null;
        String aktoerId = arbeidsforhold.getAktørId() != null ? arbeidsforhold.getAktørId().getAktørId() : null;
        return new OverstyrtUttakUtbetalingsgrad(arbeidsforhold.getType(), orgnr, aktoerId, arbeidsforhold.getInternArbeidsforholdId(), dto.getUtbetalingsgrad());
    }

    static String formater(LocalDateInterval periode) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return periode.getFomDato().format(format) + "-" + periode.getTomDato().format(format);
    }


}
