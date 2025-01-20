package no.nav.ung.sak.domene.vedtak.observer;

import static no.nav.ung.sak.domene.vedtak.observer.VedtattYtelseMapper.mapAnvisninger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.vedtak.ytelse.Aktør;
import no.nav.abakus.vedtak.ytelse.Kildesystem;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.Status;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.ytelse.DagsatsOgUtbetalingsgrad;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelseUtleder;
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseTilkjentYtelseUtleder;

@ApplicationScoped
public class VedtattYtelseTjeneste {

    private BehandlingVedtakRepository vedtakRepository;
    private TilkjentYtelseUtleder tilkjentYtelseUtleder;

    public VedtattYtelseTjeneste() {
    }

    @Inject
    public VedtattYtelseTjeneste(BehandlingVedtakRepository vedtakRepository,
                                 UngdomsytelseTilkjentYtelseUtleder utledTilkjentYtelse) {
        this.vedtakRepository = vedtakRepository;
        this.tilkjentYtelseUtleder = utledTilkjentYtelse;
    }

    public Ytelse genererYtelse(Behandling behandling) {
        final BehandlingVedtak vedtak = vedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId()).orElseThrow();
        var resultat = tilkjentYtelseUtleder.utledTilkjentYtelseTidslinje(behandling.getId());


        final Aktør aktør = new Aktør();
        aktør.setVerdi(behandling.getAktørId().getId());
        final YtelseV1 ytelse = new YtelseV1();
        ytelse.setKildesystem(Kildesystem.K9SAK);
        ytelse.setSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        ytelse.setVedtattTidspunkt(vedtak.getVedtakstidspunkt());
        ytelse.setVedtakReferanse(behandling.getUuid().toString());
        ytelse.setAktør(aktør);
        ytelse.setYtelse(mapYtelser(behandling.getFagsakYtelseType()));
        ytelse.setYtelseStatus(mapStatus(behandling.getFagsak().getStatus()));
        ytelse.setPeriode(utledPeriode(vedtak, resultat));
        ytelse.setAnvist(mapAnvisninger(resultat));
        return ytelse;
    }

    private Periode utledPeriode(BehandlingVedtak vedtak, LocalDateTimeline<DagsatsOgUtbetalingsgrad> perioder) {
        final Periode periode = new Periode();
        if (!perioder.isEmpty()) {
            periode.setFom(perioder.getMinLocalDate());
            periode.setTom(perioder.getMaxLocalDate());
            return periode;
        } else {
            periode.setFom(vedtak.getVedtaksdato());
            periode.setTom(vedtak.getVedtaksdato());
        }
        return periode;
    }

    private Ytelser mapYtelser(FagsakYtelseType type) {
        return switch (type) {
            case PLEIEPENGER_NÆRSTÅENDE -> Ytelser.PLEIEPENGER_NÆRSTÅENDE;
            case PLEIEPENGER_SYKT_BARN -> Ytelser.PLEIEPENGER_SYKT_BARN;
            case OMSORGSPENGER -> Ytelser.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> Ytelser.OPPLÆRINGSPENGER;
            case FRISINN -> Ytelser.FRISINN;
            case UNGDOMSYTELSE -> Ytelser.UNGDOMSYTELSE;
            default -> throw new IllegalStateException("Ukjent ytelsestype " + type);
        };
    }

    private Status mapStatus(FagsakStatus kode) {
        return switch (kode) {
            case OPPRETTET, UNDER_BEHANDLING -> Status.UNDER_BEHANDLING;
            case LØPENDE -> Status.LØPENDE;
            case AVSLUTTET -> Status.AVSLUTTET;
        };
    }

}
