package no.nav.k9.sak.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadAngittPersonEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.medlem.MedlemTjeneste;
import no.nav.k9.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.kontrakt.søknad.AngittPersonDto;
import no.nav.k9.sak.kontrakt.søknad.ArbeidsgiverDto;
import no.nav.k9.sak.kontrakt.søknad.ManglendeVedleggDto;
import no.nav.k9.sak.kontrakt.søknad.OppgittTilknytningDto;
import no.nav.k9.sak.kontrakt.søknad.SøknadDto;
import no.nav.k9.sak.kontrakt.søknad.UtlandsoppholdDto;
import no.nav.k9.sak.perioder.SøktPeriode;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.typer.OrganisasjonsNummerValidator;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.web.app.tjenester.behandling.søknadsfrist.SøknadsfristTjenesteProvider;

@Dependent
public class SøknadDtoTjeneste {

    private BehandlingRepositoryProvider repositoryProvider;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private MedlemTjeneste medlemTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private SøknadsfristTjenesteProvider provider;

    protected SøknadDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SøknadDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                             PersoninfoAdapter personinfoAdapter,
                             ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                             MedlemTjeneste medlemTjeneste,
                             SøknadsfristTjenesteProvider provider) {

        this.repositoryProvider = repositoryProvider;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.medlemTjeneste = medlemTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.provider = provider;
    }

    public Optional<SøknadDto> mapFra(Behandling behandling) {
        Optional<SøknadEntitet> søknadOpt = repositoryProvider.getSøknadRepository().hentSøknadHvisEksisterer(behandling.getId());
        if (søknadOpt.isPresent()) {
            SøknadEntitet søknad = søknadOpt.get();
            var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
            return lagSoknadDto(søknad, ref);
        }
        return Optional.empty();
    }

    private Optional<SøknadDto> lagSoknadDto(SøknadEntitet søknad, BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();

        var dto = new SøknadDto();
        dto.setMottattDato(søknad.getMottattDato());
        dto.setSoknadsdato(søknad.getSøknadsdato());

        // FIXME K9 sett korrekt startdato for ytelse
        dto.setOppgittStartdato(søknad.getSøknadsdato());

        dto.setTilleggsopplysninger(søknad.getTilleggsopplysninger());
        dto.setSpraakkode(søknad.getSpråkkode());
        dto.setBegrunnelseForSenInnsending(søknad.getBegrunnelseForSenInnsending());
        Optional.ofNullable(søknad.getSøknadsperiode()).ifPresent(sp -> dto.setSøknadsperiode(new Periode(sp.getFomDato(), sp.getTomDato())));

        medlemTjeneste.hentMedlemskap(behandlingId).ifPresent(ma -> {
            dto.setOppgittTilknytning(mapFra(ma.getOppgittTilknytning().orElse(null)));
        });

        dto.setManglendeVedlegg(genererManglendeVedlegg(ref));

        dto.setAngittePersoner(mapAngittePersoner(søknad.getAngittePersoner()));

        return Optional.of(dto);
    }

    public List<Periode> hentSøknadperioderPåFagsak(FagsakYtelseType ytelsetype, PersonIdent ident, PersonIdent pleietrengendeAktørIdent) {
        AktørId aktørId = finnAktørId(ident);
        AktørId pleietrengendeAktør = finnAktørId(pleietrengendeAktørIdent);

        return finnSisteFagsakPå(ytelsetype, aktørId, List.of(pleietrengendeAktør))
            .map(fagsak -> repositoryProvider.getBehandlingRepository().hentSisteBehandlingForFagsakId(fagsak.getId()))
            .map(behandling ->
                {
                    BehandlingReferanse referanse = BehandlingReferanse.fra(behandling.orElseThrow());

                    List<LocalDateTimeline<Boolean>> tidslinjer = provider.finnVurderSøknadsfristTjeneste(referanse).hentPerioderTilVurdering(referanse)
                        .values().stream().flatMap(p -> p.stream().map(SøktPeriode::getPeriode))
                        .map(dato -> new LocalDateTimeline<>(dato.toLocalDateInterval(), true)).collect(Collectors.toList());

                    return slårSammenPerioderMedHensynTilOverlapp(tidslinjer);
                }
            )
            .orElse(Collections.emptyList());
    }

    static List<Periode> slårSammenPerioderMedHensynTilOverlapp(List<LocalDateTimeline<Boolean>> tidslinjer) {
        @SuppressWarnings("unchecked")
        LocalDateTimeline<Boolean> resultat = LocalDateTimeline.EMPTY_TIMELINE;

        for (LocalDateTimeline<Boolean> localDateSegments : tidslinjer) {
            resultat = localDateSegments.combine(resultat, StandardCombinators::coalesceRightHandSide, JoinStyle.CROSS_JOIN);
        }
        return resultat
            .compress().toSegments().stream()
            .map(segment -> new Periode(segment.getFom(), segment.getTom())).collect(Collectors.toList());
    }

    private Optional<Fagsak> finnSisteFagsakPå(FagsakYtelseType ytelseType, AktørId bruker, Collection<AktørId> pleietrengendeAktørId) {
        List<Fagsak> fagsaker = repositoryProvider.getFagsakRepository().finnFagsakRelatertTilEnAvAktører(ytelseType, bruker, pleietrengendeAktørId, Collections.emptyList(), null, null);
        if (fagsaker.isEmpty()) {
            return Optional.empty();
        }
        Optional<LocalDate> sisteFomDato = fagsaker.stream().map(f -> f.getPeriode().getFomDato()).max(LocalDate::compareTo);
        return fagsaker.stream().collect(Collectors.groupingBy(f -> f.getPeriode().getFomDato())).get(sisteFomDato.get()).stream().findFirst();
    }

    private AktørId finnAktørId(PersonIdent bruker) {
        if (bruker == null)
            return null;
        return bruker.erAktørId()
            ? new AktørId(bruker.getAktørId())
            : personinfoAdapter.hentAktørIdForPersonIdent(bruker).orElseThrow(() -> new IllegalArgumentException("Finner ikke aktørId for bruker"));
    }

    private List<AngittPersonDto> mapAngittePersoner(Set<SøknadAngittPersonEntitet> angittePersoner) {
        if (angittePersoner == null || angittePersoner.isEmpty()) {
            return List.of();
        }

        var identMap = angittePersoner.stream().filter(p -> p.getAktørId() != null)
            .map(p -> new AbstractMap.SimpleEntry<>(p.getAktørId(), personinfoAdapter.hentBrukerBasisForAktør(p.getAktørId()).orElse(null)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return angittePersoner.stream()
            .map(p -> {

                var dto = new AngittPersonDto()
                    .setAktørId(p.getAktørId())
                    .setRolle(p.getRolle())
                    .setSituasjonKode(p.getSituasjonKode())
                    .setTilleggsopplysninger(p.getTilleggsopplysninger());

                var personBasis = identMap.get(p.getAktørId());
                if (personBasis != null) {
                    dto.setPersonIdent(personBasis.getPersonIdent());
                    dto.setNavn(personBasis.getNavn());
                    dto.setFødselsdato(personBasis.getFødselsdato());
                }
                return dto;
            })
            .collect(Collectors.toList());
    }

    private List<ManglendeVedleggDto> genererManglendeVedlegg(BehandlingReferanse ref) {
        Kompletthetsjekker kompletthetsjekker = Kompletthetsjekker.finnKompletthetsjekkerFor(ref.getFagsakYtelseType(), ref.getBehandlingType());
        List<ManglendeVedlegg> alleManglendeVedlegg = new ArrayList<>(kompletthetsjekker.utledAlleManglendeVedleggForForsendelse(ref));
        List<ManglendeVedlegg> vedleggSomIkkeKommer = List.copyOf(kompletthetsjekker.utledAlleManglendeVedleggSomIkkeKommer(ref));

        // Fjerner slik at det ikke blir dobbelt opp, og for å markere korrekt hvilke som ikke vil komme
        alleManglendeVedlegg.removeIf(e -> vedleggSomIkkeKommer.stream().anyMatch(it -> it.getArbeidsgiver().equals(e.getArbeidsgiver())));
        alleManglendeVedlegg.addAll(vedleggSomIkkeKommer);

        return alleManglendeVedlegg.stream().map(this::mapTilManglendeVedleggDto).collect(Collectors.toList());
    }

    private ManglendeVedleggDto mapTilManglendeVedleggDto(ManglendeVedlegg mv) {
        final ManglendeVedleggDto dto = new ManglendeVedleggDto();
        dto.setDokumentType(mv.getDokumentType());
        if (mv.getDokumentType().equals(DokumentTypeId.INNTEKTSMELDING)) {
            dto.setArbeidsgiver(mapTilArbeidsgiverDto(mv.getArbeidsgiver()));
            dto.setBrukerHarSagtAtIkkeKommer(mv.getBrukerHarSagtAtIkkeKommer() != null ? mv.getBrukerHarSagtAtIkkeKommer() : false);
        }
        return dto;
    }

    private ArbeidsgiverDto mapTilArbeidsgiverDto(String arbeidsgiverIdent) {
        if (OrganisasjonsNummerValidator.erGyldig(arbeidsgiverIdent) || OrgNummer.erKunstig(arbeidsgiverIdent)) {
            return virksomhetArbeidsgiver(new OrgNummer(arbeidsgiverIdent));
        } else {
            return privatpersonArbeidsgiver(new AktørId(arbeidsgiverIdent));
        }
    }

    private ArbeidsgiverDto privatpersonArbeidsgiver(AktørId aktørId) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(Arbeidsgiver.person(aktørId));
        ArbeidsgiverDto dto = new ArbeidsgiverDto();
        dto.setNavn(opplysninger.getNavn());
        dto.setFødselsdato(opplysninger.getFødselsdato());
        dto.setAktørId(aktørId);

        // Dette må gjøres for å ikke knekke frontend, kan fjernes når frontend er rettet.
        dto.setOrganisasjonsnummer(new OrgNummer(opplysninger.getIdentifikator()));

        return dto;
    }

    private ArbeidsgiverDto virksomhetArbeidsgiver(OrgNummer orgnr) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(Arbeidsgiver.virksomhet(orgnr));
        ArbeidsgiverDto dto = new ArbeidsgiverDto();
        dto.setOrganisasjonsnummer(orgnr);
        dto.setNavn(opplysninger.getNavn());
        return dto;
    }

    private static OppgittTilknytningDto mapFra(MedlemskapOppgittTilknytningEntitet oppgittTilknytning) {
        if (oppgittTilknytning != null) {
            return new OppgittTilknytningDto(mapOpphold(oppgittTilknytning.getOpphold()));
        }
        return null;
    }

    private static List<UtlandsoppholdDto> mapOpphold(Set<MedlemskapOppgittLandOppholdEntitet> opphold) {
        return mapFraMedlemskapOppgttLandOpphold(opphold.stream()
            .filter(o -> !o.getLand().equals(Landkoder.NOR))
            .collect(Collectors.toList()));
    }

    public static List<UtlandsoppholdDto> mapFraMedlemskapOppgttLandOpphold(List<MedlemskapOppgittLandOppholdEntitet> utlandsoppholdList) {
        return utlandsoppholdList.stream()
            .map(utlandsopphold -> new UtlandsoppholdDto(
                utlandsopphold.getLand().getNavn(),
                utlandsopphold.getPeriodeFom(),
                utlandsopphold.getPeriodeTom()))
            .collect(Collectors.toList());
    }
}
