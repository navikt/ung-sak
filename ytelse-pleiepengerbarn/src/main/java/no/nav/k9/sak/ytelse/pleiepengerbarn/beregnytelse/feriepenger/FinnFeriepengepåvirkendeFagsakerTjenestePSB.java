package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.kodeverk.Inntektskategori;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvistAndel;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.SaksnummerOgSisteBehandling;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@ApplicationScoped
public class FinnFeriepengepåvirkendeFagsakerTjenestePSB implements FinnFeriepengepåvirkendeFagsakerTjeneste {

    private FagsakRepository fagsakRepository;
    private HentFeriepengeAndelerTjeneste hentFeriepengeAndelerTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private boolean korrigerMotInfotrygd;

    FinnFeriepengepåvirkendeFagsakerTjenestePSB() {
        //for CDI proxy
    }

    @Inject
    public FinnFeriepengepåvirkendeFagsakerTjenestePSB(FagsakRepository fagsakRepository,
                                                       HentFeriepengeAndelerTjeneste hentFeriepengeAndelerTjeneste,
                                                       InntektArbeidYtelseTjeneste iayTjeneste,
                                                       @KonfigVerdi(value = "FERIEPENGER_INFOTRYGD_KORRIGER", defaultVerdi = "false") boolean korrigerMotInfotrygd) {
        this.fagsakRepository = fagsakRepository;
        this.hentFeriepengeAndelerTjeneste = hentFeriepengeAndelerTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.korrigerMotInfotrygd = korrigerMotInfotrygd;
    }

    @Override
    public LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> finnPåvirkedeSaker(BehandlingReferanse referanse) {
        LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> påvirkendeLokaleSaker = finnPåvirkendeLokaleSaker(referanse);
        if (korrigerMotInfotrygd) {
            LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> påvirkendeInfotrygdsaker = finnPåvirkendeInfotrygdsaker(referanse);
            return påvirkendeLokaleSaker.crossJoin(påvirkendeInfotrygdsaker, StandardCombinators::union);
        } else {
            return påvirkendeLokaleSaker;
        }
    }

    private LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> finnPåvirkendeLokaleSaker(BehandlingReferanse referanse) {
        Set<Fagsak> påvirkendeFagsaker = finnLokaleSakerSomPåvirkerFeriepengerFor(referanse);
        return hentFeriepengeAndelerTjeneste.finnAndelerSomKanGiFeriepenger(påvirkendeFagsaker);
    }

    private LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> finnPåvirkendeInfotrygdsaker(BehandlingReferanse referanse) {
        InntektArbeidYtelseGrunnlag iayGrunnlag = iayTjeneste.hentGrunnlag(referanse.getId());
        List<Ytelse> infotrygdYtelse = iayGrunnlag.getAktørYtelseFraRegister(referanse.getAktørId())
            .stream()
            .flatMap(ay -> ay.getAlleYtelser().stream())
            .filter(ay -> ay.getKilde() == Fagsystem.INFOTRYGD)
            .filter(ay -> ay.getYtelseType() == OPPLÆRINGSPENGER || ay.getYtelseType() == PLEIEPENGER_SYKT_BARN)
            .filter(ay -> ay.getYtelseAnvist().stream().flatMap(ya -> ya.getYtelseAnvistAndeler().stream()).anyMatch(this::kanHaFeriepenger))
            .toList();

        long dummyInfotrygdBehandlingId = 0;
        List<LocalDateSegment<Set<SaksnummerOgSisteBehandling>>> segmenter = infotrygdYtelse.stream().map(y -> new LocalDateSegment<>(y.getPeriode().toLocalDateInterval(), Set.of(new SaksnummerOgSisteBehandling(y.getSaksnummer(), dummyInfotrygdBehandlingId)))).toList();
        return new LocalDateTimeline<>(segmenter, StandardCombinators::union);
    }

    private boolean kanHaFeriepenger(YtelseAnvistAndel andel) {
        boolean inntektskategoriMedFeriepenger = andel.getInntektskategori() == Inntektskategori.ARBEIDSTAKER || andel.getInntektskategori() == Inntektskategori.SJØMANN;
        boolean harTilkjentYtelse = andel.getDagsats() != null && andel.getDagsats().getVerdi().compareTo(BigDecimal.ZERO) > 0;
        return inntektskategoriMedFeriepenger && harTilkjentYtelse;

    }

    private Set<Fagsak> finnLokaleSakerSomPåvirkerFeriepengerFor(BehandlingReferanse referanse) {
        List<Fagsak> psbFagsakerPleietrengende = fagsakRepository.finnFagsakRelatertTil(FagsakYtelseType.PSB, referanse.getAktørId(), referanse.getPleietrengendeAktørId(), null, null, null);
        List<Fagsak> oppFagsakerPleietrengende = fagsakRepository.finnFagsakRelatertTil(FagsakYtelseType.OPPLÆRINGSPENGER, referanse.getAktørId(), referanse.getPleietrengendeAktørId(), null, null, null);

        return Stream.concat(psbFagsakerPleietrengende.stream(), oppFagsakerPleietrengende.stream())
            .filter(s -> !s.getSaksnummer().equals(referanse.getSaksnummer()))
            .collect(Collectors.toSet());
    }
}
