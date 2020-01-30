package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.ppbsøknad;

import static java.util.Objects.nonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.Fordeling;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilans;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittUtenlandskVirksomhet;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.MottattDokumentWrapperSøknad;
import no.nav.k9.soknad.felles.Spraak;
import no.nav.k9.soknad.pleiepengerbarn.PleiepengerBarnSoknad;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Periode;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.AnnenOpptjening;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.EgenNaering;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Foreldrepenger;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Frilans;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.NorskOrganisasjon;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Opptjening;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Regnskapsfoerer;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.UtenlandskArbeidsforhold;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.UtenlandskOrganisasjon;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.AnnenOpptjeningTyper;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Virksomhetstyper;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Svangerskapspenger;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Person;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class PleiepengerBarnSoknadPersister {

    private VirksomhetTjeneste virksomhetTjeneste;
    private SøknadRepository søknadRepository;
    private MedlemskapRepository medlemskapRepository;
    private FordelingRepository fordelingRepository;
    private TpsTjeneste tpsTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    PleiepengerBarnSoknadPersister() {
        // for CDI proxy
    }

    @Inject
    public PleiepengerBarnSoknadPersister(BehandlingRepositoryProvider repositoryProvider,
                                          VirksomhetTjeneste virksomhetTjeneste,
                                          InntektArbeidYtelseTjeneste iayTjeneste,
                                          FordelingRepository fordelingRepository,
                                          TpsTjeneste tpsTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.fordelingRepository = fordelingRepository;
        this.tpsTjeneste = tpsTjeneste;
    }

    //@Override
    public void persister(PleiepengerBarnSoknad soknad, Behandling behandling) {
        // TODO:
        final boolean elektroniskSøknad = false;
        final SøknadEntitet.Builder søknadBuilder = new SøknadEntitet.Builder()
            .medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(soknad.getMottattDato().toLocalDate())
            .medErEndringssøknad(false)
            .medSøknadsdato(soknad.getMottattDato().toLocalDate()) // TODO: Hva er dette? Dette feltet er datoen det gjelder fra for FP-endringssøknader.
            .medSpråkkode(getSpraakValg(soknad.getSoker().getSpraakValg()));

        // Utgår for K9-ytelsene?
        //.medBegrunnelseForSenInnsending(wrapper.getBegrunnelseForSenSoeknad())
        //.medTilleggsopplysninger(wrapper.getTilleggsopplysninger())

        final Long behandlingId = behandling.getId();

        byggMedlemskap(soknad.getMedlemskap(), behandlingId, soknad.getMottattDato().toLocalDate());

        // TODO: Kan denne gjenbrukes for å angi andre søkere på samme barn?
        /*
        if (skalByggeSøknadAnnenPart(wrapper)) {
            byggSøknadAnnenPart(wrapper, behandlingId);
        }
        */

        // TODO:
        //byggOpptjeningsspesifikkeFelter(wrapper, behandlingId);

        final Set<FordelingPeriode> perioder = mapTilPerioder(soknad);
        final var fordeling = new Fordeling(perioder);
        fordelingRepository.lagreOgFlush(behandling, fordeling);

        //final RelasjonsRolleType relasjonsRolleType = utledRolle(wrapper.getBruker(), behandlingId,  behandling.getAktørId());
        final SøknadEntitet søknadEntitet = søknadBuilder
            .build();
        søknadRepository.lagreOgFlush(behandling, søknadEntitet);
        //fagsakRepository.oppdaterRelasjonsRolle(behandling.getFagsakId(), søknadEntitet.getRelasjonsRolleType());
    }

    private Set<FordelingPeriode> mapTilPerioder(PleiepengerBarnSoknad soknad) {
        return Set.of(new FordelingPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(soknad.getPeriode().getFraOgMed(), soknad.getPeriode().getTilOgMed())));
    }

    private void byggOpptjeningsspesifikkeFelter(MottattDokumentWrapperSøknad skjemaWrapper, Long behandlingId) {
        Opptjening opptjening = null;
        if (skjemaWrapper.getOmYtelse() instanceof Foreldrepenger) { // NOSONAR - ok måte å finne riktig JAXB-type
            final Foreldrepenger omYtelse = (Foreldrepenger) skjemaWrapper.getOmYtelse();
            opptjening = omYtelse.getOpptjening();
        } else if (skjemaWrapper.getOmYtelse() instanceof Svangerskapspenger) {
            final Svangerskapspenger omYtelse = (Svangerskapspenger) skjemaWrapper.getOmYtelse();
            opptjening = omYtelse.getOpptjening();
        }

        if (opptjening != null && (!opptjening.getUtenlandskArbeidsforhold().isEmpty() || !opptjening.getAnnenOpptjening().isEmpty() || !opptjening.getEgenNaering().isEmpty() || nonNull(opptjening.getFrilans()))) {
            iayTjeneste.lagreOppgittOpptjening(behandlingId, mapOppgittOpptjening(opptjening));
        }
    }

    private Arbeidsgiver oversettArbeidsgiver(no.nav.vedtak.felles.xml.soeknad.uttak.v3.Arbeidsgiver arbeidsgiverFraSøknad) {
        if (arbeidsgiverFraSøknad instanceof Person) { // NOSONAR
            Optional<AktørId> aktørId = tpsTjeneste.hentAktørForFnr(PersonIdent.fra(arbeidsgiverFraSøknad.getIdentifikator()));
            if (!aktørId.isPresent()) {
                throw new IllegalStateException("Finner ikke arbeidsgiver");
            }
            return Arbeidsgiver.person(aktørId.get());
        } else if (arbeidsgiverFraSøknad instanceof no.nav.vedtak.felles.xml.soeknad.uttak.v3.Virksomhet) { // NOSONAR
            String orgnr = arbeidsgiverFraSøknad.getIdentifikator();
            virksomhetTjeneste.hentOgLagreOrganisasjon(orgnr);
            return Arbeidsgiver.virksomhet(orgnr);
        } else {
            throw new IllegalStateException("Ukjent arbeidsgiver type " + arbeidsgiverFraSøknad.getClass());
        }
    }

    private OppgittOpptjeningBuilder mapOppgittOpptjening(Opptjening opptjening) {
        OppgittOpptjeningBuilder builder = OppgittOpptjeningBuilder.ny();
        opptjening.getAnnenOpptjening().forEach(annenOpptjening -> builder.leggTilAnnenAktivitet(mapAnnenAktivitet(annenOpptjening)));
        opptjening.getEgenNaering().forEach(egenNaering -> builder.leggTilEgneNæringer(mapEgenNæring(egenNaering)));
        opptjening.getUtenlandskArbeidsforhold().forEach(arbeidsforhold -> builder.leggTilOppgittArbeidsforhold(mapOppgittUtenlandskArbeidsforhold(arbeidsforhold)));
        if (nonNull(opptjening.getFrilans())) {
            opptjening.getFrilans().getPeriode().forEach(periode -> builder.leggTilAnnenAktivitet(mapFrilansPeriode(periode)));
            builder.leggTilFrilansOpplysninger(mapFrilansOpplysninger(opptjening.getFrilans()));
        }
        return builder;
    }

    private OppgittFrilans mapFrilansOpplysninger(Frilans frilans) {
        OppgittFrilans frilansEntitet = new OppgittFrilans();
        frilansEntitet.setErNyoppstartet(frilans.isErNyoppstartet());
        frilansEntitet.setHarInntektFraFosterhjem(frilans.isHarInntektFraFosterhjem());
        frilansEntitet.setHarNærRelasjon(frilans.isNaerRelasjon());
        frilansEntitet.setFrilansoppdrag(frilans.getFrilansoppdrag()
            .stream()
            .map(fo -> {
                OppgittFrilansoppdrag frilansoppdragEntitet = new OppgittFrilansoppdrag(fo.getOppdragsgiver(), mapPeriode(fo.getPeriode()));
                frilansoppdragEntitet.setFrilans(frilansEntitet);
                return frilansoppdragEntitet;
            }).collect(Collectors.toList()));
        return frilansEntitet;
    }

    private OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder mapOppgittUtenlandskArbeidsforhold(UtenlandskArbeidsforhold utenlandskArbeidsforhold) {
        OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder builder = OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny();
        Landkoder landkode = finnLandkode(utenlandskArbeidsforhold.getArbeidsland().getKode());
        builder.medUtenlandskVirksomhet(new OppgittUtenlandskVirksomhet(landkode, utenlandskArbeidsforhold.getArbeidsgiversnavn()));
        builder.medErUtenlandskInntekt(true);
        builder.medArbeidType(ArbeidType.UTENLANDSK_ARBEIDSFORHOLD);

        DatoIntervallEntitet periode = mapPeriode(utenlandskArbeidsforhold.getPeriode());
        builder.medPeriode(periode);
        return builder;
    }

    private OppgittAnnenAktivitet mapFrilansPeriode(Periode periode) {
        DatoIntervallEntitet datoIntervallEntitet = mapPeriode(periode);
        return new OppgittAnnenAktivitet(datoIntervallEntitet, ArbeidType.FRILANSER);
    }

    private OppgittAnnenAktivitet mapAnnenAktivitet(AnnenOpptjening annenOpptjening) {
        DatoIntervallEntitet datoIntervallEntitet = mapPeriode(annenOpptjening.getPeriode());
        AnnenOpptjeningTyper type = annenOpptjening.getType();

        ArbeidType arbeidType = ArbeidType.fraKode(type.getKode());
        return new OppgittAnnenAktivitet(datoIntervallEntitet, arbeidType);
    }

    private List<OppgittOpptjeningBuilder.EgenNæringBuilder> mapEgenNæring(EgenNaering egenNæring) {
        List<OppgittOpptjeningBuilder.EgenNæringBuilder> builders = new ArrayList<>();
        egenNæring.getVirksomhetstype().forEach(virksomhettype -> builders.add(mapEgenNæringForType(egenNæring, virksomhettype)));
        return builders;
    }

    private OppgittOpptjeningBuilder.EgenNæringBuilder mapEgenNæringForType(EgenNaering egenNæring, Virksomhetstyper virksomhettype) {
        OppgittOpptjeningBuilder.EgenNæringBuilder egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        if (egenNæring instanceof NorskOrganisasjon) {
            NorskOrganisasjon norskOrganisasjon = (NorskOrganisasjon) egenNæring;
            String orgNr = norskOrganisasjon.getOrganisasjonsnummer();
            virksomhetTjeneste.hentOgLagreOrganisasjon(orgNr);
            egenNæringBuilder.medVirksomhet(orgNr);
        } else {
            UtenlandskOrganisasjon utenlandskOrganisasjon = (UtenlandskOrganisasjon) egenNæring;
            Landkoder landkode = finnLandkode(utenlandskOrganisasjon.getRegistrertILand().getKode());
            egenNæringBuilder.medUtenlandskVirksomhet(new OppgittUtenlandskVirksomhet(landkode, utenlandskOrganisasjon.getNavn()));
        }

        // felles
        VirksomhetType virksomhetType = VirksomhetType.fraKode(virksomhettype.getKode());
        egenNæringBuilder.medPeriode(mapPeriode(egenNæring.getPeriode()))
            .medVirksomhetType(virksomhetType);

        Optional<Regnskapsfoerer> regnskapsfoerer = Optional.ofNullable(egenNæring.getRegnskapsfoerer());
        regnskapsfoerer.ifPresent(r -> egenNæringBuilder.medRegnskapsførerNavn(r.getNavn()).medRegnskapsførerTlf(r.getTelefon()));

        egenNæringBuilder.medBegrunnelse(egenNæring.getBeskrivelseAvEndring())
            .medEndringDato(egenNæring.getEndringsDato())
            .medNyoppstartet(egenNæring.isErNyoppstartet())
            .medNyIArbeidslivet(egenNæring.isErNyIArbeidslivet())
            .medVarigEndring(egenNæring.isErVarigEndring())
            .medNærRelasjon(egenNæring.isNaerRelasjon() != null && egenNæring.isNaerRelasjon());
        if (egenNæring.getNaeringsinntektBrutto() != null) {
            egenNæringBuilder.medBruttoInntekt(new BigDecimal(egenNæring.getNaeringsinntektBrutto()));
        }
        return egenNæringBuilder;
    }

    private DatoIntervallEntitet mapPeriode(Periode periode) {
        LocalDate fom = periode.getFom();
        LocalDate tom = periode.getTom();
        if (tom == null) {
            return DatoIntervallEntitet.fraOgMed(fom);
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
    }

    private void byggMedlemskap(no.nav.k9.soknad.felles.Medlemskap soknadMedlemskap, Long behandlingId, LocalDate forsendelseMottatt) {
        final MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder()
            .medOppgittDato(forsendelseMottatt);

        // TODO: Hva skal vi ha som "oppholdNå"?
        //Boolean iNorgeVedFoedselstidspunkt = medlemskap.isINorgeVedFoedselstidspunkt();
        //oppgittTilknytningBuilder.medOppholdNå(Boolean.TRUE.equals(iNorgeVedFoedselstidspunkt));

        soknadMedlemskap.getOpphold().forEach(opphold -> {
            // TODO: "tidligereOpphold" må fjernes fra database og domeneobjekter. Ved bruk må skjæringstidspunkt spesifikt oppgis.
            //boolean tidligereOpphold = opphold.getPeriode().getFom().isBefore(mottattDato);
            oppgittTilknytningBuilder.leggTilOpphold(new MedlemskapOppgittLandOppholdEntitet.Builder()
                .medLand(finnLandkode(opphold.getLand().getKode()))
                .medPeriode(
                    Objects.requireNonNullElse(opphold.getPeriode().getFraOgMed(), Tid.TIDENES_BEGYNNELSE),
                    Objects.requireNonNullElse(opphold.getPeriode().getTilOgMed(), Tid.TIDENES_ENDE)
                )
                //.erTidligereOpphold(tidligereOpphold)
                .build()
            );
        });
        medlemskapRepository.lagreOppgittTilkytning(behandlingId, oppgittTilknytningBuilder.build());
    }

    private Språkkode getSpraakValg(Spraak spraak) {
        if (spraak != null) {
            return Språkkode.fraKode(spraak.getKode().toUpperCase());
        }
        return Språkkode.UDEFINERT;
    }

    private Landkoder finnLandkode(String landKode) {
        return Landkoder.fraKode(landKode);
    }
}
