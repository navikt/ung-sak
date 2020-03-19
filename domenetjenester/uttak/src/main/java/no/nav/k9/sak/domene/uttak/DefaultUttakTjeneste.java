package no.nav.k9.sak.domene.uttak;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertLøpendeMedlemskapEntitet;
import no.nav.k9.sak.behandlingslager.behandling.pleiebehov.Pleieperiode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.uttak.input.UttakInput;
import no.nav.k9.sak.domene.uttak.repo.FeriePeriode;
import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.domene.uttak.rest.UttakRestKlient;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.LovbestemtFerie;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.UttakArbeid;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.UttakMedlemskap;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.UttakTilsynsbehov;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.UttaksplanRequest;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.Periode;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.UttakArbeidsforhold;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.UttakArbeidsforholdPeriodeInfo;
import no.nav.k9.sak.kontrakt.uttak.uttaksplan.Uttaksplan;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.util.Tuple;

@ApplicationScoped
@Default
public class DefaultUttakTjeneste implements UttakTjeneste {
    private static final Logger log = LoggerFactory.getLogger(DefaultUttakTjeneste.class);

    private UttakRestKlient restKlient;

    protected DefaultUttakTjeneste() {
    }

    @Inject
    public DefaultUttakTjeneste(UttakRestKlient uttakRestTjeneste) {
        this.restKlient = uttakRestTjeneste;
    }

    @Override
    public Uttaksplan opprettUttaksplan(UttakInput input) {
        var ref = input.getBehandlingReferanse();

        var utReq = new MapUttakRequest().nyRequest(ref, input);
        return restKlient.opprettUttaksplan(utReq);
    }

    @Override
    public boolean harAvslåttUttakPeriode(UUID behandlingUuid) {
        var uttaksplanOpt = hentUttaksplan(behandlingUuid);
        return uttaksplanOpt.map(ut -> ut.harAvslåttePerioder()).orElse(false);
    }

    @Override
    public Optional<Uttaksplan> hentUttaksplan(UUID behandlingUuid) {
        return hentUttaksplaner(behandlingUuid).values().stream().findFirst();
    }

    @Override
    public Map<UUID, Uttaksplan> hentUttaksplaner(UUID... behandlingUuid) {
        var uttaksplaner = restKlient.hentUttaksplaner(behandlingUuid).getUttaksplaner()
            .entrySet().stream().collect(Collectors.toMap(e -> UUID.fromString(e.getKey()), Map.Entry::getValue));
        return new TreeMap<>(uttaksplaner);
    }

    @Override
    public Map<Saksnummer, Uttaksplan> hentUttaksplaner(List<Saksnummer> saksnummere) {
        var uttaksplaner = restKlient.hentUttaksplaner(saksnummere).getUttaksplaner()
            .entrySet().stream().collect(Collectors.toMap(e -> new Saksnummer(e.getKey()), Map.Entry::getValue));
        return new TreeMap<>(uttaksplaner);
    }
    
    @Override
    public String hentUttaksplanerRaw(List<Saksnummer> saksnummere) {
        return restKlient.hentUttaksplanerRaw(saksnummere);
    }
    
    @Override
    public String hentUttaksplanerRaw(UUID behandlingId) {
        return restKlient.hentUttaksplanerRaw(behandlingId);
    }
    
    static class MapUttakRequest {

        // TODO K9: blir dette riktig?
        private static final Set<MedlemskapManuellVurderingType> IGNORE_PERIODER = Set.of(
            MedlemskapManuellVurderingType.SAKSBEHANDLER_SETTER_OPPHØR_AV_MEDL_PGA_ENDRINGER_I_TPS,
            MedlemskapManuellVurderingType.IKKE_RELEVANT);

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

            utReq.setMedlemskap(lagMedlemskap(input));

            // TODO K9: Er ikke implementert støtte for SN/Frilans i uttak tjeneste ennå

            // gjør en forhåndsvalidering av request
            Validate.INSTANCE.validate(utReq);

            return utReq;
        }

        private Map<Periode, UttakMedlemskap> lagMedlemskap(UttakInput input) {
            if (input.getMedlemskap() == null) {
                return null;
            }
            var maksPeriode = input.getSøknadsperioder().getMaksPeriode();

            var maksDato = maksPeriode.getTomDato();
            var perioder = input.getMedlemskap().getPerioder();
            var mapVurderingsdato = new TreeMap<>(perioder.stream()
                .map(p -> new AbstractMap.SimpleEntry<>(p.getVurderingsdato(), p))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            var mapVurderingsperioder = mapVurderingsdato.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(datoTilPeriode(mapVurderingsdato, e.getKey(), maksDato), lagUttakMedlemskap(e.getValue())))
                .filter(e -> e.getKey() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            return mapVurderingsperioder;
        }

        private UttakMedlemskap lagUttakMedlemskap(@SuppressWarnings("unused") VurdertLøpendeMedlemskapEntitet value) {
            // TODO K9: Har ikke data her foreløpig
            return new UttakMedlemskap();
        }

        private Periode datoTilPeriode(NavigableMap<LocalDate, VurdertLøpendeMedlemskapEntitet> alleDatoer, LocalDate vurderingsdato, LocalDate maksDato) {
            LocalDate tom = alleDatoer.higherKey(vurderingsdato);
            if (tom == null) {
                tom = maksDato;
            } else {
                tom = tom.minusDays(1);
            }

            var vurdering = alleDatoer.get(vurderingsdato);
            if (vurdering.getMedlemsperiodeManuellVurdering() != null && IGNORE_PERIODER.contains(vurdering.getMedlemsperiodeManuellVurdering())) {
                log.warn("Fikk medlemskapperiode: " + vurdering);
                // håndter som opphør // return null;
            }
            return new Periode(vurderingsdato, tom);
        }

        private Map<Periode, UttakTilsynsbehov> lagTilsynsbehov(UttakInput input) {
            if (input.getPleieperioder() == null) {
                return Collections.emptyMap();
            }

            var res = input.getPleieperioder().getPerioder().stream()
                .map(uap -> new AbstractMap.SimpleEntry<>(tilPeriode(uap.getPeriode()), tilUttakTilsynsbehov(uap)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            return new TreeMap<>(res);
        }

        private UttakTilsynsbehov tilUttakTilsynsbehov(Pleieperiode uap) {
            return new UttakTilsynsbehov(uap.getGrad().getProsent());
        }

        private List<LovbestemtFerie> lagLovbestemtFerie(UttakInput input) {
            if (input.getFerie() == null || input.getFerie().getPerioder() == null) {
                return Collections.emptyList();
            }
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
            if (input.getUttakAktivitetPerioder() == null) {
                return Collections.emptyMap();
            }
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
