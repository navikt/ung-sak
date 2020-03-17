package no.nav.k9.sak.domene.uttak;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.Validator;

import no.finn.unleash.Unleash;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.uttak.input.UttakInput;
import no.nav.k9.sak.domene.uttak.repo.FeriePeriode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.rest.UttakRestTjeneste;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.LovbestemtFerie;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.Periode;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.UttakArbeid;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.UttakArbeidsforhold;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.UttakArbeidsforholdPeriodeInfo;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.UttakTilsynsbehov;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.Uttaksplan;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.UttaksplanRequest;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.util.Tuple;

@ApplicationScoped
@Default
public class DefaultUttakTjeneste implements UttakTjeneste {
    private static final String FEATURE_TOGGLE = "k9.uttak.rest";

    private UttakRestTjeneste uttakRestTjeneste;
    private Unleash unleash;

    private UttakInMemoryTjeneste uttakInMemoryTjeneste = new UttakInMemoryTjeneste();

    private boolean fallbackFeatureToggleForRestEnabled;

    protected DefaultUttakTjeneste() {
    }

    @Inject
    public DefaultUttakTjeneste(UttakRestTjeneste uttakRestTjeneste,
                                Unleash unleash,
                                @KonfigVerdi(value = FEATURE_TOGGLE, required = false, defaultVerdi = "false") Boolean fallbackFeatureToggleForRestEnabled) {
        this.uttakRestTjeneste = uttakRestTjeneste;
        this.unleash = unleash;
        this.fallbackFeatureToggleForRestEnabled = fallbackFeatureToggleForRestEnabled;
    }

    @Override
    public Uttaksplan opprettUttaksplan(UttakInput input) {
        var ref = input.getBehandlingReferanse();

        var utReq = new MapUttakRequest().nyRequest(ref, input);

        if (isRestEnabled()) {
            // FIXME K9: Fjern feature toggle når uttak tjeneste er oppe
            return uttakRestTjeneste.opprettUttaksplan(utReq);
        } else {
            return uttakInMemoryTjeneste.opprettUttaksplan(input);
        }
    }

    private boolean isRestEnabled() {
        return fallbackFeatureToggleForRestEnabled || unleash.isEnabled(FEATURE_TOGGLE);
    }

    @Override
    public boolean harAvslåttUttakPeriode(UUID behandlingUuid) {
        var uttaksplanOpt = hentUttaksplan(behandlingUuid);
        return uttaksplanOpt.map(ut -> ut.harAvslåttePerioder()).orElse(false);
    }

    @Override
    public Optional<Uttaksplan> hentUttaksplan(UUID behandlingUuid) {
        return hentUttaksplaner(behandlingUuid).stream().findFirst();
    }

    @Override
    public List<Uttaksplan> hentUttaksplaner(UUID... behandlingUuid) {
        if (isRestEnabled()) {
            // FIXME K9: Fjern feature toggle når uttak tjeneste er oppe
            return uttakRestTjeneste.hentUttaksplaner(behandlingUuid);
        } else {
            return uttakInMemoryTjeneste.hentUttaksplaner(behandlingUuid);
        }
    }

    static class MapUttakRequest {

        UttaksplanRequest nyRequest(BehandlingReferanse ref, UttakInput input) {
            var utReq = new UttaksplanRequest();
            utReq.setBarn(input.getPleietrengende());
            utReq.setSøker(input.getSøker());
            utReq.setBehandlingId(ref.getBehandlingUuid());
            utReq.setSaksnummer(ref.getSaksnummer());

            utReq.setSøknadsperioder(lagSøknadsperioder(input));
            utReq.setArbeid(mapArbeid(input));
            utReq.setLovbestemtFerie(lagLovbestemtFerie(input));
            utReq.setTilsynsbehov(lagTilsynsbehov(input));

            // TODO K9: Er ikke implementert støtte for SN/Frilans i uttak tjeneste ennå

            Validate.INSTANCE.validate(utReq);

            return utReq;
        }

        private Map<Periode, UttakTilsynsbehov> lagTilsynsbehov(UttakInput input) {
            // TODO K9: hvordan skal dette håndters ? fra fastsatt uttak?
            return Map.of();
        }

        private List<LovbestemtFerie> lagLovbestemtFerie(UttakInput input) {
            return input.getFerie().getPerioder().stream()
                .map(FeriePeriode::getPeriode)
                .map(p -> new LovbestemtFerie(tilPeriode(p)))
                .collect(Collectors.toList());
        }

        private List<Periode> lagSøknadsperioder(UttakInput input) {
            return input.getSøknadsperioder().getPerioder().stream()
                .map(Søknadsperiode::getPeriode)
                .map(MapUttakRequest::tilPeriode)
                .collect(Collectors.toList());
        }

        private List<UttakArbeid> mapArbeid(UttakInput input) {
            var mappedArbeidPerioder = mappedAktivitet(input);
            var arbeid = mappedArbeidPerioder.entrySet().stream()
                .map(e -> mapArbeid(e.getKey().getElement1(), e.getKey().getElement2(), UttakArbeidType.ARBEIDSTAKER, e.getValue()))
                .collect(Collectors.toList());
            return arbeid;
        }

        private UttakArbeid mapArbeid(Arbeidsgiver arbeidsgiver,
                                      InternArbeidsforholdRef arbeidsforholdRef,
                                      UttakArbeidType uttakArbeidType,
                                      Collection<UttakAktivitetPeriode> aktiviteter) {
            var ua = new UttakArbeid();
            ua.setArbeidsforhold(mapUttakArbeidsforhold(arbeidsgiver, arbeidsforholdRef, uttakArbeidType));

            var aktivitetMap = aktiviteter.stream()
                .map(uap -> new AbstractMap.SimpleEntry<>(tilPeriode(uap.getPeriode()), tilArbeidsforholdPeriodeInfo(uap)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            ua.setPerioder(aktivitetMap);

            return ua;
        }

        private UttakArbeidsforholdPeriodeInfo tilArbeidsforholdPeriodeInfo(UttakAktivitetPeriode uap) {
            return new UttakArbeidsforholdPeriodeInfo(uap.getJobberNormaltPerUke(), uap.getSkalJobbeProsent());
        }

        private static Periode tilPeriode(DatoIntervallEntitet key) {
            return new Periode(key.getFomDato(), key.getTomDato());
        }

        private UttakArbeidsforhold mapUttakArbeidsforhold(Arbeidsgiver arb, InternArbeidsforholdRef arbeidsforholdRef, UttakArbeidType aktivitetType) {
            var internArbRef = arbeidsforholdRef == null ? null : arbeidsforholdRef.getReferanse();
            return new UttakArbeidsforhold(
                arb == null ? null : arb.getOrgnr(),
                arb == null ? null : arb.getAktørId(),
                aktivitetType,
                internArbRef);
        }

        private Map<Tuple<Arbeidsgiver, InternArbeidsforholdRef>, List<UttakAktivitetPeriode>> mappedAktivitet(UttakInput input) {
            return input.getUttakAktivitetPerioder()
                .stream().filter(a -> UttakArbeidType.ARBEIDSTAKER.equals(a.getAktivitetType()) && a.getArbeidsgiver() != null)
                .collect(Collectors.groupingBy(a -> new Tuple<>(a.getArbeidsgiver(), a.getArbeidsforholdRef() != null ? a.getArbeidsforholdRef() : InternArbeidsforholdRef.nullRef())));
        }

    }

    static class Validate {
        // late initialize pattern - see Josh Bloch effective java
        private static final Validate INSTANCE = new Validate();
        private Validator validator;

        Validate() {
            try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
                this.validator = validatorFactory.getValidator();
            }
        }

        void validate(Object obj) {
            var constraints = this.validator.validate(obj);
            if (!constraints.isEmpty()) {
                throw new IllegalArgumentException("Kan ikke validate obj=" + obj + "\n\tValidation errors:" + constraints);
            }
        }
    }
}
