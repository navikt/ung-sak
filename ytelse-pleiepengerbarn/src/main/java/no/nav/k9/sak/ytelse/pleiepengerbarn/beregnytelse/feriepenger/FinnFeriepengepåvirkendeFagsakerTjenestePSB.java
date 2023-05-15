package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.kodeverk.Inntektskategori;
import no.nav.fpsak.tidsserie.LocalDateInterval;
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
import no.nav.k9.sak.domene.iay.modell.YtelseAnvist;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvistAndel;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.InfotrygdFeriepengegrunnlag;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.SaksnummerOgSisteBehandling;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@ApplicationScoped
public class FinnFeriepengepåvirkendeFagsakerTjenestePSB implements FinnFeriepengepåvirkendeFagsakerTjeneste {

    private FagsakRepository fagsakRepository;
    private HentFeriepengeAndelerTjeneste hentFeriepengeAndelerTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private boolean korrigerMotInfotrygd;

    /**
     * periode hvor feriepenger skal samkjøres mellom k9 og infotrygd, ved at det korrigeres fra k9-siden.
     */
    private static final LocalDateInterval SAMKJØRINGSPERIODE = new LocalDateInterval(LocalDate.of(2022, 1, 1), LocalDate.of(2023, 12, 31));

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

    @Override
    public InfotrygdFeriepengegrunnlag finnInfotrygdFeriepengegrunnlag(BehandlingReferanse referanse) {
        if (korrigerMotInfotrygd) {
            List<InfotrygdFeriepengegrunnlag.InfotrygdFeriepengegrunnlagAndel> andeler = new ArrayList<>();
            InntektArbeidYtelseGrunnlag iayGrunnlag = iayTjeneste.hentGrunnlag(referanse.getId());
            List<Ytelse> ytelser = iayGrunnlag.getAktørYtelseFraRegister(referanse.getAktørId())
                .stream()
                .flatMap(ay -> ay.getAlleYtelser().stream())
                .filter(ay -> ay.getKilde() == Fagsystem.INFOTRYGD)
                .filter(ay -> ay.getYtelseType() == OPPLÆRINGSPENGER || ay.getYtelseType() == PLEIEPENGER_SYKT_BARN)
                .toList();

            for (Ytelse ytelse : ytelser) {
                Saksnummer saksnummer = ytelse.getSaksnummer();
                for (YtelseAnvist anvist : ytelse.getYtelseAnvist()) {
                    LocalDateInterval anvistPeriode = new LocalDateInterval(anvist.getAnvistFOM(), anvist.getAnvistTOM());
                    Optional<LocalDateInterval> overlapp = anvistPeriode.overlap(SAMKJØRINGSPERIODE);
                    if (overlapp.isEmpty()) {
                        continue;
                    }
                    for (YtelseAnvistAndel andel : anvist.getYtelseAnvistAndeler()) {
                        boolean inntektskategoriMedFeriepenger = andel.getInntektskategori() == Inntektskategori.ARBEIDSTAKER || andel.getInntektskategori() == Inntektskategori.SJØMANN;
                        if (inntektskategoriMedFeriepenger) {
                            BigDecimal dagsatsRefusjon = andel.getDagsats().getVerdi().multiply(andel.getRefusjonsgradProsent().getVerdi()).setScale(2, RoundingMode.HALF_UP);
                            BigDecimal dagsatsBruker = andel.getDagsats().getVerdi().subtract(dagsatsRefusjon);
                            Arbeidsgiver arbeidsgiver = andel.getArbeidsgiver().orElse(null);
                            andeler.add(new InfotrygdFeriepengegrunnlag.InfotrygdFeriepengegrunnlagAndel(overlapp.get(), saksnummer, arbeidsgiver, dagsatsBruker, dagsatsRefusjon));
                        }
                    }
                }
            }
            return new InfotrygdFeriepengegrunnlag(andeler);
        } else {
            return null;
        }
    }

    private LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> finnPåvirkendeLokaleSaker(BehandlingReferanse referanse) {
        Set<Fagsak> påvirkendeFagsaker = finnLokaleSakerSomPåvirkerFeriepengerFor(referanse);
        return hentFeriepengeAndelerTjeneste.finnAndelerSomKanGiFeriepenger(påvirkendeFagsaker);
    }

    private LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> finnPåvirkendeInfotrygdsaker(BehandlingReferanse referanse) {
        InfotrygdFeriepengegrunnlag grunnlag = finnInfotrygdFeriepengegrunnlag(referanse);
        return grunnlag != null
            ? grunnlag.saksnummerTidslinje()
            : LocalDateTimeline.empty();
    }


    private Set<Fagsak> finnLokaleSakerSomPåvirkerFeriepengerFor(BehandlingReferanse referanse) {
        List<Fagsak> psbFagsakerPleietrengende = fagsakRepository.finnFagsakRelatertTil(FagsakYtelseType.PSB, referanse.getAktørId(), referanse.getPleietrengendeAktørId(), null, null, null);
        List<Fagsak> oppFagsakerPleietrengende = fagsakRepository.finnFagsakRelatertTil(FagsakYtelseType.OPPLÆRINGSPENGER, referanse.getAktørId(), referanse.getPleietrengendeAktørId(), null, null, null);

        return Stream.concat(psbFagsakerPleietrengende.stream(), oppFagsakerPleietrengende.stream())
            .filter(s -> !s.getSaksnummer().equals(referanse.getSaksnummer()))
            .collect(Collectors.toSet());
    }
}
