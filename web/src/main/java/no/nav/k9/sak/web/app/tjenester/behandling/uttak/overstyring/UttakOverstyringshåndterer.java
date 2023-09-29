package no.nav.k9.sak.web.app.tjenester.behandling.uttak.overstyring;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkEndretFeltType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
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
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakArbeidsforholdDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakPeriodeDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakSlettPeriodeDto;
import no.nav.k9.sak.kontrakt.uttak.overstyring.OverstyrUttakUtbetalingsgradDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrUttakDto.class, adapter = Overstyringshåndterer.class)
public class UttakOverstyringshåndterer implements Overstyringshåndterer<OverstyrUttakDto> {

    private OverstyrUttakRepository overstyrUttakRepository;
    private HistorikkRepository historikkRepository;

    UttakOverstyringshåndterer() {
        //for CDI proxy
    }

    @Inject
    public UttakOverstyringshåndterer(OverstyrUttakRepository overstyrUttakRepository, HistorikkRepository historikkRepository) {
        this.overstyrUttakRepository = overstyrUttakRepository;
        this.historikkRepository = historikkRepository;
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyrUttakDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        LocalDateTimeline<OverstyrtUttakPeriode> overstyringerFørOppdatering = overstyrUttakRepository.hentOverstyrtUttak(behandling.getId());

        LocalDateTimeline<OverstyrtUttakPeriode> oppdateringer = new LocalDateTimeline<>(dto.getLagreEllerOppdater().stream().map(this::mapTilSegment).toList());
        List<Long> perioderTilSletting = dto.getSlett().stream().map(OverstyrUttakSlettPeriodeDto::getId).toList();
        overstyrUttakRepository.oppdaterOverstyringAvUttak(behandling.getId(), perioderTilSletting, oppdateringer);

        LocalDateTimeline<OverstyrtUttakPeriode> overstyringerEtterOppdatering = overstyrUttakRepository.hentOverstyrtUttak(behandling.getId());

        lagreHistorikkinnslagForEndringer(behandling.getId(), overstyringerFørOppdatering, overstyringerEtterOppdatering);

        if (dto.gåVidere()) {
            return OppdateringResultat.builder().build();
        } else {
            return OppdateringResultat.builder().medEkstraAksjonspunktResultat(AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAK, AksjonspunktStatus.OPPRETTET).build();
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
        LocalDateTimeline<OverstyrtUttakPeriode> slettedeOverstyringer = overstyringerFørOppdatering.disjoint(overstyringerEtterOppdatering);
        LocalDateTimeline<OverstyrtUttakPeriode> nyeOverstyringer = overstyringerEtterOppdatering.disjoint(overstyringerFørOppdatering);
        LocalDateTimeline<OverstyrtUttakPeriode> oppdaterteOverstyringer = overstyringerEtterOppdatering.disjoint(nyeOverstyringer);

        lagreHistorikkinnslag(behandingId, nyeOverstyringer, HistorikkinnslagType.OVST_UTTAK_NY);
        lagreHistorikkinnslag(behandingId, slettedeOverstyringer, HistorikkinnslagType.OVST_UTTAK_FJERNET);
        lagreHistorikkinnslag(behandingId, oppdaterteOverstyringer, HistorikkinnslagType.OVST_UTTAK_OPPDATERT);
    }

    private void lagreHistorikkinnslag(Long behandlingId, LocalDateTimeline<OverstyrtUttakPeriode> overstyring, HistorikkinnslagType aksjon) {
        overstyring.forEach(segment -> {
            Historikkinnslag innslag = new Historikkinnslag();
            innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
            innslag.setBehandlingId(behandlingId);
            innslag.setType(aksjon);

            HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder().medSkjermlenke(SkjermlenkeType.UTTAK);
            tekstBuilder.medHendelse(innslag.getType());
            tekstBuilder.medEndretFelt(HistorikkEndretFeltType.UTTAK_OVERSTYRT_PERIODE, null, formater(segment.getLocalDateInterval()));
            tekstBuilder.medBegrunnelse(segment.getValue().getBegrunnelse());
            tekstBuilder.build(innslag);
            historikkRepository.lagre(innslag);
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

    String formater(LocalDateInterval periode) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return periode.getFomDato().format(format) + "-" + periode.getTomDato().format(format);
    }


}
