package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.kontrakt.uttak.Periode;
import no.nav.k9.sak.kontrakt.uttak.UttakArbeidsforhold;
import no.nav.k9.sak.kontrakt.uttak.UttaksperiodeOmsorgspenger;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumRequest;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;

@ApplicationScoped
@Default
public class ÅrskvantumTjenesteImpl implements ÅrskvantumTjeneste {

    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private ÅrskvantumKlient årskvantumKlient;

    @Inject
    public ÅrskvantumTjenesteImpl(OmsorgspengerGrunnlagRepository grunnlagRepository,
                                  ÅrskvantumRestKlient årskvantumRestKlient) {
        this.grunnlagRepository = grunnlagRepository;
        this.årskvantumKlient = årskvantumRestKlient;

    }

    @Override
    public ÅrskvantumResultat hentÅrskvantumUttak(BehandlingReferanse ref) {

        var årskvantumRequest = new ÅrskvantumRequest();

        var grunnlag = grunnlagRepository.hentOppgittFravær(ref.getBehandlingId());

        årskvantumRequest.setBehandlingId(ref.getBehandlingId().toString());
        årskvantumRequest.setAktørid(ref.getAktørId().getId());
        for (OppgittFraværPeriode fraværPeriode : grunnlag.getPerioder()) {
            UttaksperiodeOmsorgspenger uttaksperiodeOmsorgspenger = new UttaksperiodeOmsorgspenger(new Periode(fraværPeriode.getFom(), fraværPeriode.getTom()),
                null,
                null,
                fraværPeriode.getFraværPerDag());
            Arbeidsgiver arb = fraværPeriode.getArbeidsgiver();

            if (arb == null) {
                uttaksperiodeOmsorgspenger.setUttakArbeidsforhold(new UttakArbeidsforhold(null, null, fraværPeriode.getAktivitetType(), null));
            } else {
                String arbeidsforholdId = fraværPeriode.getArbeidsforholdRef() == null ? null : fraværPeriode.getArbeidsforholdRef().getReferanse();
                uttaksperiodeOmsorgspenger.setUttakArbeidsforhold(new UttakArbeidsforhold(arb.getOrgnr(),
                    arb.getAktørId(),
                    fraværPeriode.getAktivitetType(),
                    arbeidsforholdId));
            }
            årskvantumRequest.getUttaksperioder().add(uttaksperiodeOmsorgspenger);
        }
        return årskvantumKlient.hentÅrskvantumUttak(årskvantumRequest);
    }

}
