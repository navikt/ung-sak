package no.nav.ung.sak.formidling;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.Sats;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersonBasisTjeneste;
import no.nav.ung.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.ung.sak.formidling.domene.GenerertBrev;
import no.nav.ung.sak.formidling.domene.GrunnlagOgTilkjentYtelse;
import no.nav.ung.sak.formidling.domene.PdlPerson;
import no.nav.ung.sak.formidling.dto.Brevbestilling;
import no.nav.ung.sak.formidling.pdfgen.PdfGenDokument;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.template.TemplateInput;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.template.dto.felles.FellesDto;
import no.nav.ung.sak.formidling.template.dto.felles.MottakerDto;
import no.nav.ung.sak.formidling.template.dto.felles.PeriodeDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.GbeløpPeriodeDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.InnvilgelseDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.ResultatFlaggDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.SatserDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.TilkjentPeriodeDto;
import no.nav.ung.sak.formidling.template.dto.innvilgelse.TilkjentYtelseDto;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.ytelse.DagsatsOgUtbetalingsgrad;
import no.nav.ung.sak.ytelse.beregning.TilkjentYtelseUtleder;

@ApplicationScoped
public class BrevGenerererTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(BrevGenerererTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private PersonBasisTjeneste personBasisTjeneste;
    private AktørTjeneste aktørTjeneste;
    private PdfGenKlient pdfGen;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private TilkjentYtelseUtleder tilkjentYtelseUtleder;

    @Inject
    public BrevGenerererTjeneste(
        BehandlingRepository behandlingRepository,
        PersonBasisTjeneste personBasisTjeneste,
        AktørTjeneste aktørTjeneste,
        PdfGenKlient pdfGen,
        UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
        UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
        TilkjentYtelseUtleder tilkjentYtelseUtleder) {
        this.behandlingRepository = behandlingRepository;
        this.personBasisTjeneste = personBasisTjeneste;
        this.aktørTjeneste = aktørTjeneste;
        this.pdfGen = pdfGen;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.tilkjentYtelseUtleder = tilkjentYtelseUtleder;
    }

    public BrevGenerererTjeneste() {
    }

    public GenerertBrev genererVedtaksbrev(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (!behandling.erAvsluttet()) {
            throw new IllegalStateException("Behandling må være avsluttet for å kunne bestille vedtaksbrev");
        }
        LocalDateTimeline<DagsatsOgUtbetalingsgrad> tilkjentYtelseTidslinje = tilkjentYtelseUtleder.utledTilkjentYtelseTidslinje(behandlingId);
        if (tilkjentYtelseTidslinje.isEmpty()) {
            LOG.warn("Behandling har ingen tilkjent ytelse. Støtter ikke vedtaksbrev for avslag foreløpig. BehandlingResultat={}", behandling.getBehandlingResultatType());
            return null;
        }

        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandlingId).orElseThrow(() -> new IllegalStateException("Mangler grunnlag"));
        var satsTidslinje = ungdomsytelseGrunnlag.getSatsTidslinje();

        var pdlMottaker = hentMottaker(behandling);
        var periode = new PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(260));

        var grunnlagOgTilkjentYtelseTimeline = tilkjentYtelseTidslinje.intersection(satsTidslinje, sammenstillGrunnlagOgTilkjentYtelse())
            .compress();

        var sortertGrunnlagOgTilkjentYtelseVerdier = grunnlagOgTilkjentYtelseTimeline.stream()
            .sorted(Comparator.comparing(LocalDateSegment::getLocalDateInterval, Comparator.reverseOrder())) // nyeste først
            .map(LocalDateSegment::getValue)
            .distinct()
            .toList();

        Set<TilkjentPeriodeDto> tilkjentePerioder = grunnlagOgTilkjentYtelseTimeline
            .mapSegment(
                it1 -> new TilkjentYtelseDto(
                    it1.dagsats(),
                    it1.grunnbeløpFaktor(),
                    it1.grunnbeløp(),
                    it1.årsbeløp()
                ))
            .compress().stream().map(
                it -> new TilkjentPeriodeDto(
                    new PeriodeDto(it.getFom(), it.getTom()),
                    it.getValue()
                )
            ).collect(Collectors.toSet());


        var satsTyper = sortertGrunnlagOgTilkjentYtelseVerdier.stream().map(GrunnlagOgTilkjentYtelse::satsType).collect(Collectors.toSet());

        var nyesteLavSats = sortertGrunnlagOgTilkjentYtelseVerdier.stream()
            .filter(it -> it.satsType() == UngdomsytelseSatsType.LAV)
            .findFirst()
            .map(GrunnlagOgTilkjentYtelse::grunnbeløpFaktor);

        var nyesteHøySats = sortertGrunnlagOgTilkjentYtelseVerdier.stream()
            .filter(it -> it.satsType() == UngdomsytelseSatsType.HØY)
            .findFirst()
            .map(GrunnlagOgTilkjentYtelse::grunnbeløpFaktor);

        var gBeløpPerioder = grunnlagOgTilkjentYtelseTimeline
            .mapSegment(GrunnlagOgTilkjentYtelse::grunnbeløp)
            .compress().stream()
            .map(it -> new GbeløpPeriodeDto(
                new PeriodeDto(it.getFom(), it.getTom()), it.getValue()))
            .collect(Collectors.toSet());

        var helger = Hjelpetidslinjer.lagTidslinjeMedKunHelger(tilkjentYtelseTidslinje);
        var antallDager = tilkjentYtelseTidslinje.disjoint(helger).getLocalDateIntervals().stream().mapToLong(LocalDateInterval::totalDays).sum();

        var input = new TemplateInput(TemplateType.INNVILGELSE,
            new InnvilgelseDto(
                FellesDto.automatisk(new MottakerDto(pdlMottaker.navn(), pdlMottaker.fnr())),
                new ResultatFlaggDto(sortertGrunnlagOgTilkjentYtelseVerdier.stream().collect(Collectors.groupingBy(GrunnlagOgTilkjentYtelse::dagsats)).size() == 1,
                    sortertGrunnlagOgTilkjentYtelseVerdier.stream().collect(Collectors.groupingBy(GrunnlagOgTilkjentYtelse::grunnbeløp)).size() == 1,
                    satsTyper.stream().allMatch(it -> it == UngdomsytelseSatsType.LAV),
                    satsTyper.stream().allMatch(it -> it == UngdomsytelseSatsType.HØY),
                    satsTyper.contains(UngdomsytelseSatsType.LAV) && satsTyper.contains(UngdomsytelseSatsType.HØY),
                    grunnlagOgTilkjentYtelseTimeline.getMaxLocalDate().isAfter(pdlMottaker.fødselsdato().plusYears(Sats.HØY.getTomAlder()))),
                grunnlagOgTilkjentYtelseTimeline.getMinLocalDate(),
                antallDager, // TODO regne ut
                tilkjentePerioder,
                gBeløpPerioder,
                new SatserDto(nyesteHøySats.orElse(null), nyesteLavSats.orElse(null), Sats.LAV.getTomAlder(), Sats.HØY.getTomAlder())));


        // konverter til pdf fra templateData
         PdfGenDokument dokument = pdfGen.lagDokument(input);

        return new GenerertBrev(
            dokument,
            pdlMottaker,
            pdlMottaker,
           DokumentMalType.INNVILGELSE_DOK,
            input.templateType()
        );
    }

    private static LocalDateSegmentCombinator<DagsatsOgUtbetalingsgrad, UngdomsytelseSatser, GrunnlagOgTilkjentYtelse> sammenstillGrunnlagOgTilkjentYtelse() {
        return (di, lhs, rhs) -> {
            var dg = lhs.getValue();
            var sp = rhs.getValue();
            return new LocalDateSegment<>(di,
                new GrunnlagOgTilkjentYtelse(
                    dg.dagsats(),
                    avrundTilHeltall(dg.utbetalingsgrad()),
                    sp.satsType(),
                    sp.grunnbeløpFaktor().setScale(2, RoundingMode.HALF_UP),
                    avrundTilHeltall(sp.grunnbeløp()).longValue(),
                    avrundTilHeltall(sp.grunnbeløp().multiply(sp.grunnbeløpFaktor())).longValue(),
                    sp.antallBarn(),
                    sp.dagsatsBarnetillegg()
                ));

        };
    }


    private static BigDecimal avrundTilHeltall(BigDecimal decimal) {
        return decimal.setScale(0, RoundingMode.HALF_UP);
    }

    public GenerertBrev generer(Brevbestilling brevbestilling) {
        Behandling behandling = behandlingRepository.hentBehandling(brevbestilling.behandlingId());
        var pdlMottaker = hentMottaker(behandling);

        // valider mal via regel hvis vedtaksbrev

        // lag brev json via datasamler og velg templateData
        PeriodeDto periode = new PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(260));
        var input = new TemplateInput(TemplateType.INNVILGELSE,
            new InnvilgelseDto(
                FellesDto.automatisk(new MottakerDto(pdlMottaker.navn(), pdlMottaker.fnr())),
                new ResultatFlaggDto(true,
                    true,
                    true,
                    false,
                    false,
                    false),
                LocalDate.now(),
                500,
                Set.of(
                    new TilkjentPeriodeDto(
                        periode,
                        new TilkjentYtelseDto(
                            400,
                            BigDecimal.valueOf(4.22),
                            200000,
                            100000)
                    )
                ),
                Set.of(
                    new GbeløpPeriodeDto(periode, 200000)
                ),
                new SatserDto(BigDecimal.TWO, BigDecimal.TEN, 10, 20)));


        // konverter til pdf fra templateData
        PdfGenDokument dokument = pdfGen.lagDokument(input);

        return new GenerertBrev(
            dokument,
            pdlMottaker,
            pdlMottaker,
            brevbestilling.malType(),
            input.templateType()
        );
    }

    private PdlPerson hentMottaker(Behandling behandling) {
        AktørId aktørId = behandling.getFagsak().getAktørId();
        var personIdent = aktørTjeneste.hentPersonIdentForAktørId(aktørId)
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke person med aktørid"));
        PersoninfoBasis personinfoBasis = personBasisTjeneste.hentBasisPersoninfo(aktørId, personIdent);

        String fnr = personIdent.getIdent();
        Objects.requireNonNull(fnr);
        return new PdlPerson(fnr, aktørId, personinfoBasis.getNavn(), personinfoBasis.getFødselsdato());
    }


}

