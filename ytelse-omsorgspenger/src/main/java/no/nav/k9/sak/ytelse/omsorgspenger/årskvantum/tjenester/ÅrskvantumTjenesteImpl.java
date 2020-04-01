package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;


import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.kontrakt.uttak.*;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumRequest;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumKlient;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.rest.ÅrskvantumRestKlient;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
            uttaksperiodeOmsorgspenger.setUttakArbeidsforhold(new UttakArbeidsforhold(fraværPeriode.getArbeidsgiver().getOrgnr(),
                                                                                      fraværPeriode.getArbeidsgiver().getAktørId(),
                                                                                      UttakArbeidType.ARBEIDSTAKER,//TODO finn ut av hvordan vi kan utlede denne
                                                                                      null)); //TODO sjekk opp om denne trengs
            årskvantumRequest.getUttaksperioder().add(uttaksperiodeOmsorgspenger);
        }
        return årskvantumKlient.hentÅrskvantumUttak(årskvantumRequest);
    }

}
