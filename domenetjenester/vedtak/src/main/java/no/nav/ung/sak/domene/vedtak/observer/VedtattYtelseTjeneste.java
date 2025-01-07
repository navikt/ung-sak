package no.nav.ung.sak.domene.vedtak.observer;

import static no.nav.ung.sak.domene.vedtak.observer.VedtattYtelseMapper.mapAnvisninger;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.vedtak.ytelse.Aktør;
import no.nav.abakus.vedtak.ytelse.Kildesystem;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.Status;
import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelsePeriode;
import no.nav.ung.sak.ytelse.beregning.UngdomsytelseUtledTilkjentYtelse;
import no.nav.ung.sak.ytelse.beregning.UtledTilkjentYtelse;

@ApplicationScoped
public class VedtattYtelseTjeneste {

    private BehandlingVedtakRepository vedtakRepository;
    private UtledTilkjentYtelse utledTilkjentYtelse;

    public VedtattYtelseTjeneste() {
    }

    @Inject
    public VedtattYtelseTjeneste(BehandlingVedtakRepository vedtakRepository,
                                 UngdomsytelseUtledTilkjentYtelse utledTilkjentYtelse) {
        this.vedtakRepository = vedtakRepository;
        this.utledTilkjentYtelse = utledTilkjentYtelse;
    }

    public Ytelse genererYtelse(Behandling behandling) {
        final BehandlingVedtak vedtak = vedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId()).orElseThrow();
        var resultat = utledTilkjentYtelse.utledTilkjentYtelsePerioder(behandling.getId());


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
        ytelse.setPeriode(utledPeriode(vedtak, resultat.orElse(null)));
        ytelse.setAnvist(mapAnvisninger(resultat.orElse(null)));
        return ytelse;
    }

    private Periode utledPeriode(BehandlingVedtak vedtak, List<TilkjentYtelsePeriode> perioder) {
        final Periode periode = new Periode();
        if (perioder != null) {
            Optional<LocalDate> minFom = perioder.stream()
                .map(TilkjentYtelsePeriode::periode)
                .map(DatoIntervallEntitet::getFomDato)
                .min(Comparator.naturalOrder());
            Optional<LocalDate> maxTom = perioder.stream()
                .map(TilkjentYtelsePeriode::periode)
                .map(DatoIntervallEntitet::getTomDato)
                .max(Comparator.naturalOrder());
            if (minFom.isEmpty()) {
                periode.setFom(vedtak.getVedtaksdato());
                periode.setTom(vedtak.getVedtaksdato());
                return periode;
            }
            periode.setFom(minFom.get());
            if (maxTom.isPresent()) {
                periode.setTom(maxTom.get());
            } else {
                periode.setTom(Tid.TIDENES_ENDE);
            }
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
