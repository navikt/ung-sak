package no.nav.ung.sak.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentTypeId;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadAngittPersonEntitet;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.ung.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersoninfoAdapter;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kompletthet.Kompletthetsjekker;
import no.nav.ung.sak.kompletthet.ManglendeVedlegg;
import no.nav.ung.sak.kontrakt.søknad.AngittPersonDto;
import no.nav.ung.sak.kontrakt.søknad.ArbeidsgiverDto;
import no.nav.ung.sak.kontrakt.søknad.ManglendeVedleggDto;
import no.nav.ung.sak.kontrakt.søknad.SøknadDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.OrgNummer;
import no.nav.ung.sak.typer.OrganisasjonsNummerValidator;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.typer.Saksnummer;

@Dependent
public class SøknadDtoTjeneste {

    private BehandlingRepositoryProvider repositoryProvider;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    protected SøknadDtoTjeneste() {
        // for CDI proxy
    }

    @Inject
    public SøknadDtoTjeneste(BehandlingRepositoryProvider repositoryProvider,
                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                             PersoninfoAdapter personinfoAdapter,
                             ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                             @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester) {

        this.repositoryProvider = repositoryProvider;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
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

        dto.setManglendeVedlegg(genererManglendeVedlegg(ref));

        dto.setAngittePersoner(mapAngittePersoner(søknad.getAngittePersoner()));

        return Optional.of(dto);
    }

    public List<Periode> hentSøknadperioderPåFagsak(FagsakYtelseType ytelsetype, PersonIdent ident, PersonIdent pleietrengendeAktørIdent) {
        AktørId aktørId = finnAktørId(ident);
        AktørId pleietrengendeAktør = pleietrengendeAktørIdent != null ? finnAktørId(pleietrengendeAktørIdent) : null;
        ytelsetype.validerNøkkelParametere(pleietrengendeAktør, null);

        final Optional<Fagsak> fagsakOpt = finnSisteFagsakPå(ytelsetype, aktørId, pleietrengendeAktør);
        return hentFagsakPerioder(fagsakOpt);
    }

    private List<Periode> hentFagsakPerioder(Optional<Fagsak> fagsakOpt) {
        return fagsakOpt
            .flatMap(fagsak -> repositoryProvider.getBehandlingRepository().hentSisteBehandlingForFagsakId(fagsak.getId()))
            .map(behandling -> {
                    final var vilkårsPerioderTilVurderingTjeneste = finnVilkårsPerioderTilVurderingTjeneste(behandling.getFagsak().getYtelseType(), behandling.getType());
                    final NavigableSet<DatoIntervallEntitet> søknadsperioder = vilkårsPerioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId());
                    return søknadsperioder != null ? søknadsperioder.stream().map(p -> new Periode(p.getFomDato(), p.getTomDato())).collect(Collectors.toList()) : new ArrayList<Periode>();
                }
            )
            .orElse(Collections.emptyList());
    }

    public List<Periode> hentSøknadperioderPåFagsak(Saksnummer saksnummer) {
        final Optional<Fagsak> fagsakOpt = repositoryProvider.getFagsakRepository().hentSakGittSaksnummer(saksnummer);
        return hentFagsakPerioder(fagsakOpt);
    }

    private Optional<Fagsak> finnSisteFagsakPå(FagsakYtelseType ytelseType, AktørId bruker, AktørId pleietrengendeAktørId) {
        final List<Fagsak> fagsaker = repositoryProvider.getFagsakRepository().finnFagsakRelatertTil(ytelseType, bruker, pleietrengendeAktørId, null, null, null);
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

        var identMap = angittePersoner.stream()
            .map(SøknadAngittPersonEntitet::getAktørId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toMap(Function.identity(), aktørId -> personinfoAdapter.hentBrukerBasisForAktør(aktørId).orElseThrow(() -> new IllegalArgumentException("Fant ikke informasjon for person på saken"))));

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
            .toList();
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
            dto.setArbeidsgiver(mapTilArbeidsgiverDto(mv.getArbeidsgiver().getIdentifikator()));
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

        return dto;
    }

    private ArbeidsgiverDto virksomhetArbeidsgiver(OrgNummer orgnr) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(Arbeidsgiver.virksomhet(orgnr));
        ArbeidsgiverDto dto = new ArbeidsgiverDto();
        dto.setOrganisasjonsnummer(orgnr);
        dto.setNavn(opplysninger.getNavn());
        return dto;
    }

    private VilkårsPerioderTilVurderingTjeneste finnVilkårsPerioderTilVurderingTjeneste(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjenester, ytelseType, behandlingType);
    }
}
