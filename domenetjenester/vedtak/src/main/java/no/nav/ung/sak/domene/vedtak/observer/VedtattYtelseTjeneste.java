package no.nav.ung.sak.domene.vedtak.observer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.abakus.vedtak.ytelse.*;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.ung.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.ung.sak.domene.vedtak.observer.VedtattYtelseMapper.mapAnvisninger;

@ApplicationScoped
public class VedtattYtelseTjeneste {

    private BehandlingVedtakRepository vedtakRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private Instance<YtelseTilleggsopplysningerTjeneste> tilleggsopplysningerTjenester;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    public VedtattYtelseTjeneste() {
    }

    @Inject
    public VedtattYtelseTjeneste(BehandlingVedtakRepository vedtakRepository, BeregningsresultatRepository beregningsresultatRepository,
                                 @Any Instance<YtelseTilleggsopplysningerTjeneste> tilleggsopplysningerTjenester, InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.vedtakRepository = vedtakRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.tilleggsopplysningerTjenester = tilleggsopplysningerTjenester;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    public Ytelse genererYtelse(Behandling behandling) {
        final BehandlingVedtak vedtak = vedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId()).orElseThrow();
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentEndeligBeregningsresultat(behandling.getId());
        List<ArbeidsforholdReferanse> arbeidsforholdReferanser = inntektArbeidYtelseTjeneste.hentGrunnlag(behandling.getId())
            .getArbeidsforholdInformasjon()
            .stream()
            .flatMap(a -> a.getArbeidsforholdReferanser().stream())
            .collect(Collectors.toList());

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
        finnTjeneste(behandling.getFagsakYtelseType())
            .ifPresent(it -> ytelse.setTilleggsopplysninger(JsonObjectMapper.toJson(it.generer(behandling),
                PubliserVedtakHendelseFeil.FEILFACTORY::kanIkkeSerialisere)));

        ytelse.setPeriode(utledPeriode(vedtak, berResultat.orElse(null)));
        ytelse.setAnvist(mapAnvisninger(berResultat.orElse(null), arbeidsforholdReferanser));
        return ytelse;
    }

    private Periode utledPeriode(BehandlingVedtak vedtak, BeregningsresultatEntitet beregningsresultat) {
        final Periode periode = new Periode();
        if (beregningsresultat != null) {
            Optional<LocalDate> minFom = beregningsresultat.getBeregningsresultatPerioder().stream()
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
                .min(Comparator.naturalOrder());
            Optional<LocalDate> maxTom = beregningsresultat.getBeregningsresultatPerioder().stream()
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom)
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


    private Optional<YtelseTilleggsopplysningerTjeneste> finnTjeneste(FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(tilleggsopplysningerTjenester, fagsakYtelseType);
    }
}
