package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFravær;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

/** Samle sammen fakta for fravær. */
@ApplicationScoped
@BehandlingStegRef(kode = "KOFAKUT")
@BehandlingTypeRef
@FagsakYtelseTypeRef("OMP")
public class FaktaForÅrskvantumUttakSteg implements BehandlingSteg {

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    protected FaktaForÅrskvantumUttakSteg() {
        // for proxy
    }

    @Inject
    public FaktaForÅrskvantumUttakSteg(OmsorgspengerGrunnlagRepository grunnlagRepository,
                                       InntektArbeidYtelseTjeneste iayTjeneste,
                                       InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.grunnlagRepository = grunnlagRepository;
        this.iayTjeneste = iayTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        AktørId aktørId = kontekst.getAktørId();

        var samletFravær = samleSammenOppgittFravær(behandlingId, aktørId);
        grunnlagRepository.lagreOgFlushOppgittFravær(behandlingId, samletFravær);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private OppgittFravær samleSammenOppgittFravær(Long behandlingId, AktørId aktørId) {
        Set<OppgittFraværPeriode> fravær = new LinkedHashSet<>();
        var oppgittOpt = annetOppgittFravær(behandlingId);
        if (oppgittOpt.isPresent()) {
            fravær.addAll(oppgittOpt.get().getPerioder());
        }
        var fraværFraInntektsmeldinger = fraværFraInntektsmeldinger(behandlingId, aktørId);
        fravær.addAll(fraværFraInntektsmeldinger);
        return new OppgittFravær(fravær);
    }

    private Optional<OppgittFravær> annetOppgittFravær(Long behandlingId) {
        return grunnlagRepository.hentOppgittFraværHvisEksisterer(behandlingId);
    }

    private List<OppgittFraværPeriode> fraværFraInntektsmeldinger(Long behandlingId, AktørId aktørId) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(aktørId, skjæringstidspunkt, iayGrunnlag);
        return trekkUtFravær(inntektsmeldinger);
    }

    private List<OppgittFraværPeriode> trekkUtFravær(List<Inntektsmelding> inntektsmeldinger) {
        var fravær = inntektsmeldinger.stream()
            .map(Inntektsmelding::getOppgittFravær)
            .flatMap(Collection::stream)
            .map(pa -> new OppgittFraværPeriode(pa.getFom(), pa.getTom(), UttakArbeidType.ARBEIDSTAKER, pa.getVarighetPerDag()))
            .collect(Collectors.toList());
        return fravær;
    }

}
