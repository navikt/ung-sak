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
import no.nav.k9.sak.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
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
public class UttakOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyrUttakDto> {

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
        LocalDateTimeline<OverstyrtUttakPeriode> oppdateringer = new LocalDateTimeline<>(dto.getLagreEllerOppdater().stream().map(this::mapTilSegment).toList());
        List<Long> perioderTilSletting = dto.getSlett().stream().map(OverstyrUttakSlettPeriodeDto::getId).toList();
        overstyrUttakRepository.oppdaterOverstyringer(behandling.getId(), perioderTilSletting, oppdateringer);

        if (dto.gåVidere()) {
            return OppdateringResultat.builder().build();
        } else {
            return OppdateringResultat.builder().medEkstraAksjonspunktResultat(AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAK, AksjonspunktStatus.OPPRETTET).build();
        }
    }

    private LocalDateSegment<OverstyrtUttakPeriode> mapTilSegment(OverstyrUttakPeriodeDto periodeDto) {
        return new LocalDateSegment<>(periodeDto.getPeriode().getFom(), periodeDto.getPeriode().getTom(), map(periodeDto));
    }


    private OverstyrtUttakPeriode map(OverstyrUttakPeriodeDto dto) {
        Set<OverstyrtUttakUtbetalingsgrad> utbetalingsgrader = dto.getUtbetalingsgrader().stream().map(this::map).collect(Collectors.toSet());
        return new OverstyrtUttakPeriode(dto.getId(), dto.getSøkersUttakgsgrad(), utbetalingsgrader);
    }

    private OverstyrtUttakUtbetalingsgrad map(OverstyrUttakUtbetalingsgradDto dto) {
        OverstyrUttakArbeidsforholdDto arbeidsforhold = dto.getArbeidsforhold();
        String orgnr = arbeidsforhold.getOrgnr() != null ? arbeidsforhold.getOrgnr().getOrgNummer() : null;
        String aktoerId = arbeidsforhold.getAktørId() != null ? arbeidsforhold.getAktørId().getAktørId() : null;
        return new OverstyrtUttakUtbetalingsgrad(arbeidsforhold.getType(), orgnr, aktoerId, arbeidsforhold.getInternArbeidsforholdId(), dto.getUtbetalingsgrad());
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyrUttakDto dto) {
        if (!dto.gåVidere()) {
            return; //unngår å opprette historikkinnslag inntil overstyrer fullfører aksjonspunktet
        }
        LocalDateTimeline<OverstyrtUttakPeriode> overstyringer = overstyrUttakRepository.hentOverstyrtUttak(behandling.getId());

        Historikkinnslag innslag = new Historikkinnslag();
        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        innslag.setBehandlingId(behandling.getId());
        innslag.setType(HistorikkinnslagType.OVST_UTTAK);

        HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder().medSkjermlenke(SkjermlenkeType.UTTAK);
        tekstBuilder.medHendelse(innslag.getType());
        tekstBuilder.medEndretFelt(HistorikkEndretFeltType.UTTAK_OVERSTYRT_PERIODE, null, String.join(", ", overstyringer.stream().map(LocalDateSegment::getLocalDateInterval).map(this::formater).toList()));
        tekstBuilder.medBegrunnelse(dto.getBegrunnelse());
        tekstBuilder.build(innslag);
        historikkRepository.lagre(innslag);
    }

    String formater(LocalDateInterval periode) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        return periode.getFomDato().format(format) + "-" + periode.getTomDato().format(format);
    }


}
