package no.nav.ung.sak.domene.vedtak.samordning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.vedtak.ytelse.Aktør;
import no.nav.abakus.vedtak.ytelse.Desimaltall;
import no.nav.abakus.vedtak.ytelse.Kildesystem;
import no.nav.abakus.vedtak.ytelse.Periode;
import no.nav.abakus.vedtak.ytelse.Status;
import no.nav.abakus.vedtak.ytelse.Ytelser;
import no.nav.abakus.vedtak.ytelse.request.VedtakForPeriodeRequest;
import no.nav.abakus.vedtak.ytelse.v1.YtelseV1;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Anvisning;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.AnvistAndel;
import no.nav.abakus.vedtak.ytelse.v1.anvisning.Inntektklasse;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.samordning.VedtakYtelse;
import no.nav.ung.sak.behandlingslager.samordning.VedtakYtelseRepository;
import no.nav.ung.sak.behandlingslager.samordning.YtelseAnvist;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Henter lagrede AVP- og UPY-vedtak for samordning med andre ytelser, i abakus-formatet ({@link YtelseV1}).
 * <p>
 * Kun gjeldende (aktive) vedtak med minst én innvilget periode som overlapper forespørselen tas med.
 * Periodene kuttes ikke. Status utledes fra fagsakens nåværende status ved oppslag.
 */
@ApplicationScoped
public class SamordningYtelseTjeneste {

    private static final Map<FagsakYtelseType, Ytelser> STØTTEDE_YTELSER = Map.of(
        FagsakYtelseType.UNGDOMSYTELSE, Ytelser.UNGDOMSYTELSE,
        FagsakYtelseType.AKTIVITETSPENGER, Ytelser.AKTIVITETSPENGER);

    private VedtakYtelseRepository vedtakYtelseRepository;
    private FagsakRepository fagsakRepository;
    private AktørTjeneste aktørTjeneste;

    SamordningYtelseTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SamordningYtelseTjeneste(VedtakYtelseRepository vedtakYtelseRepository,
                                    FagsakRepository fagsakRepository,
                                    AktørTjeneste aktørTjeneste) {
        this.vedtakYtelseRepository = vedtakYtelseRepository;
        this.fagsakRepository = fagsakRepository;
        this.aktørTjeneste = aktørTjeneste;
    }

    public List<YtelseV1> hentVedtak(VedtakForPeriodeRequest request) {
        var ytelseTyper = utledYtelseTyper(request.getYtelser());
        if (ytelseTyper.isEmpty()) {
            return List.of();
        }
        var aktørIder = utledAktørIder(request.getIdent());
        if (aktørIder.isEmpty()) {
            return List.of();
        }

        var forespurtPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(request.getPeriode().getFom(), request.getPeriode().getTom());
        return vedtakYtelseRepository.hentAktiveVedtakSomOverlapper(aktørIder, ytelseTyper, forespurtPeriode.getFomDato(), forespurtPeriode.getTomDato())
            .stream()
            .filter(vedtak -> harAnvisningSomOverlapper(vedtak, forespurtPeriode))
            .map(this::tilYtelseV1)
            .toList();
    }

    /**
     * {@link VedtakForPeriodeRequest#ALLE_YTELSER} er standardverdien når konsumenten ikke oppgir ytelser,
     * men den inneholder ikke UNG-ytelsene. Da tolkes forespørselen som «alle ytelser vi har», på samme måte som
     * en tom liste.
     */
    static Set<FagsakYtelseType> utledYtelseTyper(Set<Ytelser> forespurteYtelser) {
        if (forespurteYtelser == null || forespurteYtelser.isEmpty() || forespurteYtelser.equals(VedtakForPeriodeRequest.ALLE_YTELSER)) {
            return EnumSet.copyOf(STØTTEDE_YTELSER.keySet());
        }
        return STØTTEDE_YTELSER.entrySet().stream()
            .filter(e -> forespurteYtelser.contains(e.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(FagsakYtelseType.class)));
    }

    private Set<AktørId> utledAktørIder(Aktør ident) {
        if (ident.erAktørId()) {
            return Set.of(new AktørId(ident.getVerdi()));
        }
        return aktørTjeneste.hentAlleAktørIderForPersonIdent(new PersonIdent(ident.getVerdi()));
    }

    private static boolean harAnvisningSomOverlapper(VedtakYtelse vedtak, DatoIntervallEntitet forespurtPeriode) {
        return vedtak.getAnvisninger().stream().anyMatch(a -> a.getPeriode().overlapper(forespurtPeriode));
    }

    private YtelseV1 tilYtelseV1(VedtakYtelse vedtak) {
        var aktør = new Aktør();
        aktør.setVerdi(vedtak.getAktørId().getId());

        var ytelse = new YtelseV1();
        ytelse.setAktør(aktør);
        ytelse.setSaksnummer(vedtak.getSaksnummer().getVerdi());
        ytelse.setYtelse(STØTTEDE_YTELSER.get(vedtak.getYtelseType()));
        ytelse.setYtelseStatus(utledStatus(vedtak));
        ytelse.setKildesystem(Kildesystem.UNGSAK);
        ytelse.setVedtattTidspunkt(vedtak.getVedtattTidspunkt());
        ytelse.setVedtakReferanse(vedtak.getVedtakReferanse().toString());
        ytelse.setPeriode(tilPeriode(vedtak.getPeriode()));
        ytelse.setAnvist(vedtak.getAnvisninger().stream().map(SamordningYtelseTjeneste::tilAnvisning).toList());
        return ytelse;
    }

    private Status utledStatus(VedtakYtelse vedtak) {
        return fagsakRepository.hentSakGittSaksnummer(vedtak.getSaksnummer())
            .map(Fagsak::getStatus)
            .map(SamordningYtelseTjeneste::tilStatus)
            .orElse(Status.UKJENT);
    }

    private static Status tilStatus(FagsakStatus status) {
        return switch (status) {
            case OPPRETTET, UNDER_BEHANDLING -> Status.UNDER_BEHANDLING;
            case LØPENDE -> Status.LØPENDE;
            case AVSLUTTET -> Status.AVSLUTTET;
        };
    }

    private static Anvisning tilAnvisning(YtelseAnvist ytelseAnvist) {
        var anvisning = new Anvisning();
        anvisning.setPeriode(tilPeriode(ytelseAnvist.getPeriode()));
        anvisning.setDagsats(ytelseAnvist.getDagsats().map(Desimaltall::new).orElse(null));
        anvisning.setBeløp(ytelseAnvist.getBeløp().map(Desimaltall::new).orElse(null));
        anvisning.setUtbetalingsgrad(ytelseAnvist.getUtbetalingsgrad().map(Desimaltall::new).orElse(null));
        anvisning.setAndeler(ytelseAnvist.getDagsats()
            .map(dagsats -> List.of(new AnvistAndel(null, null,
                new Desimaltall(dagsats),
                ytelseAnvist.getUtbetalingsgrad().map(Desimaltall::new).orElse(null),
                new Desimaltall(BigDecimal.ZERO),
                Inntektklasse.ARBEIDSTAKER_UTEN_FERIEPENGER)))
            .orElse(List.of()));
        return anvisning;
    }

    private static Periode tilPeriode(DatoIntervallEntitet intervall) {
        var periode = new Periode();
        periode.setFom(intervall.getFomDato());
        periode.setTom(intervall.getTomDato());
        return periode;
    }
}
