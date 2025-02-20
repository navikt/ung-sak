package no.nav.ung.sak.test.util.behandling;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import jakarta.persistence.EntityManager;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.geografisk.Landkoder;
import no.nav.ung.kodeverk.geografisk.Region;
import no.nav.ung.kodeverk.person.PersonstatusType;
import no.nav.ung.kodeverk.person.SivilstandType;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.Behandling.Builder;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagBuilder;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårsResultat;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.behandlingslager.diff.DiffResult;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakLås;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakTestUtil;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatsResultat;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.Whitebox;
import no.nav.ung.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.ung.sak.test.util.behandling.personopplysning.Personopplysning;
import no.nav.ung.sak.test.util.behandling.personopplysning.Personstatus;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;

/**
 * Default test scenario builder for å definere opp testdata med enkle defaults.
 * <p>
 * Oppretter en default behandling, inkludert default grunnlag med søknad + tomt innangsvilkårresultat.
 * <p>
 * Kan bruke settere (evt. legge til) for å tilpasse utgangspunktet.
 * <p>
 * Mer avansert bruk er ikke gitt at kan bruke denne
 * klassen.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractTestScenario<S extends AbstractTestScenario<S>> {

    private static final AtomicLong FAKE_ID = new AtomicLong(100999L);
    private final FagsakBuilder fagsakBuilder;
    private final Map<Long, PersonopplysningGrunnlagEntitet> personopplysningMap = new IdentityHashMap<>();
    private final Map<Long, Behandling> behandlingMap = new HashMap<>();
    private ArgumentCaptor<Behandling> behandlingCaptor = ArgumentCaptor.forClass(Behandling.class);
    private ArgumentCaptor<Fagsak> fagsakCaptor = ArgumentCaptor.forClass(Fagsak.class);
    private Behandling behandling;

    private Fagsak fagsak;
    private SøknadEntitet.Builder søknadBuilder;

    private BehandlingVedtak.Builder behandlingVedtakBuilder;
    private BehandlingStegType startSteg;

    private Map<AksjonspunktDefinisjon, BehandlingStegType> aksjonspunktDefinisjoner = new HashMap<>();
    private List<VilkårData> vilkår = new ArrayList<>();
    private Long fagsakId = nyId();
    private LocalDate behandlingstidFrist;
    private LocalDateTime opplysningerOppdatertTidspunkt;
    private String behandlendeEnhet;
    private BehandlingRepository mockBehandlingRepository;
    private BehandlingVedtak behandlingVedtak;
    private BehandlingType behandlingType = BehandlingType.FØRSTEGANGSSØKNAD;

    // Registret og overstyrt personinfo
    private List<PersonInformasjon> personer;

    private Behandling originalBehandling;
    private BehandlingÅrsakType behandlingÅrsakType;
    private BehandlingRepositoryProvider repositoryProvider;
    private PersonInformasjon.Builder personInformasjonBuilder;
    private boolean manueltOpprettet;
    private BehandlingResultatType behandlingResultatType = BehandlingResultatType.IKKE_FASTSATT;
    private BehandlingStatus behandlingStatus = BehandlingStatus.UTREDES; // vanligste for tester
    private UngTestScenario ungTestscenario;

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType) {
        this.fagsakBuilder = FagsakBuilder
            .nyFagsak(fagsakYtelseType)
            .medSaksnummer(new Saksnummer(nyId() + ""));
    }

    protected AbstractTestScenario(FagsakYtelseType fagsakYtelseType, AktørId aktørId) {
        this.fagsakBuilder = FagsakBuilder
            .nyFagsak(fagsakYtelseType)
            .medSaksnummer(new Saksnummer(nyId() + ""))
            .medBruker(aktørId);
    }

    static long nyId() {
        return FAKE_ID.getAndIncrement();
    }

    private BehandlingRepository lagBasicMockBehandlingRepository(BehandlingRepositoryProvider repositoryProvider) {
        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);

        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);

        FagsakRepository mockFagsakRepository = mockFagsakRepository();
        PersonopplysningRepository mockPersonopplysningRepository = lagMockPersonopplysningRepository();
        SøknadRepository søknadRepository = mockSøknadRepository();
        FagsakLåsRepository fagsakLåsRepository = mockFagsakLåsRepository();
        VilkårResultatRepository vilkårResultatRepository = mockVilkårResultatRepository();

        BehandlingLåsRepository behandlingLåsReposiory = mockBehandlingLåsRepository();

        BehandlingVedtakRepository behandlingVedtakRepository = mockBehandlingVedtakRepository();
        // ikke ideelt å la mocks returnere mocks, men forenkler enormt mye test kode, forhindrer feil oppsett, så det
        // blir enklere å refactorere

        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getFagsakRepository()).thenReturn(mockFagsakRepository);
        when(repositoryProvider.getPersonopplysningRepository()).thenReturn(mockPersonopplysningRepository);
        when(repositoryProvider.getSøknadRepository()).thenReturn(søknadRepository);
        when(repositoryProvider.getBehandlingVedtakRepository()).thenReturn(behandlingVedtakRepository);
        when(repositoryProvider.getFagsakLåsRepository()).thenReturn(fagsakLåsRepository);
        when(repositoryProvider.getBehandlingLåsRepository()).thenReturn(behandlingLåsReposiory);
        when(repositoryProvider.getVilkårResultatRepository()).thenReturn(vilkårResultatRepository);

        return behandlingRepository;
    }

    private VilkårResultatRepository mockVilkårResultatRepository() {
        return new VilkårResultatRepository() {
            private Map<Long, VilkårsResultat> entiteter = new HashMap<>();

            @Override
            public Optional<Vilkårene> hentHvisEksisterer(Long behandlingId) {
                return Optional.ofNullable(entiteter.get(behandlingId)).map(VilkårsResultat::getVilkårene);
            }

            @Override
            public void lagre(Long behandlingId, Vilkårene resultat) {
                entiteter.put(behandlingId, new VilkårsResultat(behandlingId, resultat));
            }

            @Override
            public Vilkårene hent(Long behandlingId) {
                return Optional.ofNullable(entiteter.get(behandlingId)).map(VilkårsResultat::getVilkårene).orElseThrow();
            }

            @Override
            public void kopier(Long fraBehandlingId, Long tilBehandlingId) {
                entiteter.put(fraBehandlingId, new VilkårsResultat(tilBehandlingId, hent(tilBehandlingId)));
            }
        };
    }

    private BehandlingLåsRepository mockBehandlingLåsRepository() {
        return new BehandlingLåsRepository() {

            @Override
            public BehandlingLås taLås(Long behandlingId) {
                return new BehandlingLås(behandlingId);
            }

            @Override
            public void oppdaterLåsVersjon(BehandlingLås behandlingLås) {
            }

            @Override
            public BehandlingLås taLås(UUID eksternBehandlingRef) {
                return null;
            }
        };
    }

    private FagsakLåsRepository mockFagsakLåsRepository() {
        return new FagsakLåsRepository() {
            @Override
            public FagsakLås taLås(Long fagsakId) {
                return new FagsakLås(fagsakId) {

                };
            }

            @Override
            public FagsakLås taLås(Fagsak fagsak) {
                return new FagsakLås(fagsak.getId()) {

                };
            }

            @Override
            public void oppdaterLåsVersjon(FagsakLås fagsakLås) {

            }
        };
    }

    private BehandlingVedtakRepository mockBehandlingVedtakRepository() {
        BehandlingVedtakRepository behandlingVedtakRepository = mock(BehandlingVedtakRepository.class);
        BehandlingVedtak behandlingVedtak = mockBehandlingVedtak();
        when(behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(Mockito.any())).thenReturn(Optional.of(behandlingVedtak));

        return behandlingVedtakRepository;
    }

    private BehandlingVedtak mockBehandlingVedtak() {
        if (behandlingVedtak == null) {
            behandlingVedtak = Mockito.mock(BehandlingVedtak.class);
        }
        return behandlingVedtak;
    }

    private SøknadRepository mockSøknadRepository() {
        return new SøknadRepository() {

            private SøknadEntitet søknad;

            @Override
            public SøknadEntitet hentSøknad(Behandling behandling1) {
                return søknad;
            }

            @Override
            public Optional<SøknadEntitet> hentSøknadHvisEksisterer(Long behandlingId) {
                return Optional.ofNullable(søknad);
            }

            @Override
            public void lagreOgFlush(Behandling behandling, SøknadEntitet søknad1) {
                this.søknad = søknad1;
            }

        };
    }

    /**
     * Hjelpe metode for å håndtere mock repository.
     */
    public BehandlingRepository mockBehandlingRepository() {
        if (mockBehandlingRepository != null) {
            return mockBehandlingRepository;
        }
        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        BehandlingRepository behandlingRepository = lagBasicMockBehandlingRepository(repositoryProvider);

        when(behandlingRepository.hentBehandling(Mockito.any(Long.class))).thenAnswer(a -> {
            Long id = a.getArgument(0);
            return behandlingMap.getOrDefault(id, null);
        });
        when(behandlingRepository.hentBehandling(Mockito.any(UUID.class))).thenAnswer(a -> {
            throw new UnsupportedOperationException("Ikke implementert for AbstractTestScenario");
        });
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(Mockito.any())).thenAnswer(a -> {
            return List.copyOf(behandlingMap.values());
        });
        when(behandlingRepository.hentBehandlingHvisFinnes(Mockito.anyLong())).thenAnswer(a -> {
            Long id = a.getArgument(0);
            return Optional.ofNullable(behandlingMap.getOrDefault(id, null));
        });
        when(behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(Mockito.any(), Mockito.any(BehandlingType.class)))
            .thenAnswer(a -> {
                Long id = a.getArgument(0);
                BehandlingType type = a.getArgument(1);
                return behandlingMap.values().stream().filter(b -> type.equals(b.getType()) && b.getFagsakId().equals(id)).sorted().findFirst();
            });
        when(behandlingRepository.hentSisteBehandlingForFagsakId(Mockito.any()))
            .thenAnswer(a -> {
                Long id = a.getArgument(0);
                return behandlingMap.values().stream().filter(b -> b.getFagsakId().equals(id)).sorted().findFirst();
            });
        when(behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(Mockito.any()))
            .thenAnswer(a -> {
                Long id = a.getArgument(0);
                return behandlingMap.values().stream()
                    .filter(b -> b.getFagsakId().equals(id) && b.getBehandlingResultatType().isBehandlingHenlagt()).sorted()
                    .findFirst();
            });

        when(behandlingRepository.taSkriveLås(behandlingCaptor.capture())).thenAnswer((Answer<BehandlingLås>) invocation -> {
            Behandling beh = invocation.getArgument(0);
            return new BehandlingLås(beh.getId()) {
            };
        });

        when(behandlingRepository.hentSistOppdatertTidspunkt(Mockito.any()))
            .thenAnswer(a -> Optional.ofNullable(opplysningerOppdatertTidspunkt));

        when(behandlingRepository.lagre(behandlingCaptor.capture(), Mockito.any()))
            .thenAnswer((Answer<Long>) invocation -> {
                Behandling beh = invocation.getArgument(0);
                Long id = beh.getId();
                if (id == null) {
                    id = nyId();
                    Whitebox.setInternalState(beh, "id", id);
                }

                beh.getAksjonspunkter().forEach(punkt -> Whitebox.setInternalState(punkt, "id", nyId()));
                behandlingMap.put(id, beh);
                return id;
            });

        mockBehandlingRepository = behandlingRepository;
        return behandlingRepository;
    }

    public BehandlingRepositoryProvider mockBehandlingRepositoryProvider() {
        mockBehandlingRepository();
        return repositoryProvider;
    }

    private PersonopplysningRepository lagMockPersonopplysningRepository() {
        return new MockPersonopplysningRepository();
    }

    private FagsakRepository mockFagsakRepository() {
        FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        when(fagsakRepository.finnEksaktFagsak(Mockito.anyLong())).thenAnswer(a -> fagsak);
        when(fagsakRepository.finnUnikFagsak(Mockito.anyLong())).thenAnswer(a -> Optional.of(fagsak));
        when(fagsakRepository.hentSakGittSaksnummer(Mockito.any(Saksnummer.class))).thenAnswer(a -> Optional.of(fagsak));
        when(fagsakRepository.hentForBruker(Mockito.any(AktørId.class))).thenAnswer(a -> singletonList(fagsak));
        when(fagsakRepository.opprettNy(fagsakCaptor.capture())).thenAnswer(invocation -> {
            Fagsak fagsak = invocation.getArgument(0); // NOSONAR
            Long id = fagsak.getId();
            if (id == null) {
                id = fagsakId;
                Whitebox.setInternalState(fagsak, "id", id);
            }
            return id;
        });

        // oppdater fagsakstatus
        Mockito.doAnswer(invocation -> {
                FagsakStatus status = invocation.getArgument(1);
                Whitebox.setInternalState(fagsak, "fagsakStatus", status);
                return null;
            }).when(fagsakRepository)
            .oppdaterFagsakStatus(eq(fagsakId), Mockito.any(FagsakStatus.class));

        return fagsakRepository;
    }

    public Fagsak lagreFagsak(BehandlingRepositoryProvider repositoryProvider) {
        lagFagsak(repositoryProvider.getFagsakRepository());
        return fagsak;
    }

    public Behandling lagre(EntityManager entityManager) {
        return lagre(new BehandlingRepositoryProvider(entityManager));
    }

    public Behandling lagre(BehandlingRepositoryProvider repositoryProvider) {

        build(repositoryProvider.getBehandlingRepository(), repositoryProvider);
        return behandling;
    }

    @SuppressWarnings("unchecked")
    public S medUngTestGrunnlag(UngTestScenario ungTestscenario) {
        this.ungTestscenario = ungTestscenario;
        return (S) this;
    }

    public UngTestScenario getUngTestGrunnlag() {
        return ungTestscenario;
    }

    public Behandling buildOgLagreMedUng(UngTestRepositories repositories) {
        if (ungTestscenario == null)
            throw new IllegalArgumentException("ungTestGrunnlag må settes for å bruke buildUng");

        // Default Person
        if (personer == null) {
            var ungdom = getDefaultBrukerAktørId();
            PersonInformasjon personInformasjon = opprettBuilderForRegisteropplysninger()
                .medPersonas()
                .ungdom(ungdom, ungTestscenario.fødselsdato(), ungTestscenario.navn())
                .statsborgerskap(Landkoder.NOR)
                .personstatus(PersonstatusType.BOSA)
                .build();
            medRegisterOpplysninger(personInformasjon);
        }

        //Vilkår
        if (ungTestscenario.aldersvilkår() != null) {
            ungTestscenario.aldersvilkår().forEach(it -> leggTilVilkår(VilkårType.ALDERSVILKÅR, it.getValue(), new Periode(it.getFom(), it.getTom())));
        }

        if (ungTestscenario.ungdomsprogramvilkår() != null) {
            ungTestscenario.ungdomsprogramvilkår().forEach(it -> leggTilVilkår(VilkårType.UNGDOMSPROGRAMVILKÅRET, it.getValue(), new Periode(it.getFom(), it.getTom())));
        }

        build(repositories.repositoryProvider().getBehandlingRepository(), repositories.repositoryProvider());

        //Ung ting
        buildUng(repositories);
        return behandling;
    }

    private void buildUng(UngTestRepositories repositories) {

        if (ungTestscenario.satser() != null) {
            repositories.ungdomsytelseGrunnlagRepository().lagre(behandling.getId(), new UngdomsytelseSatsResultat(
                ungTestscenario.satser(),
                "regelInputSats",
                "regelSporing"
            ));
        }

        if (ungTestscenario.uttakPerioder() != null) {
            repositories.ungdomsytelseGrunnlagRepository().lagre(behandling.getId(), ungTestscenario.uttakPerioder());
        }

        if (ungTestscenario.programPerioder() != null) {
            repositories.ungdomsprogramPeriodeRepository().lagre(behandling.getId(), ungTestscenario.programPerioder());

        }

        if (ungTestscenario.søknadStartDato() != null) {
            List<UngdomsytelseSøktStartdato> starDatoer = ungTestscenario.søknadStartDato().stream().map(it -> new UngdomsytelseSøktStartdato(it, new JournalpostId("123")))
                .toList();
            repositories.ungdomsytelseStartdatoRepository().lagre(behandling.getId(), starDatoer);
            repositories.ungdomsytelseStartdatoRepository().lagreRelevanteSøknader(behandling.getId(), new UngdomsytelseStartdatoer(starDatoer));
        }

        if (ungTestscenario.tilkjentYtelsePerioder() != null) {
            repositories.tilkjentYtelseRepository().lagre(behandling.getId(), ungTestscenario.tilkjentYtelsePerioder(), "input", "sporing");
        }

        if (ungTestscenario.behandlingTriggere() != null && repositories.prosessTriggereRepository() != null) {
            repositories.prosessTriggereRepository().leggTil(behandling.getId(), ungTestscenario.behandlingTriggere());
        }

    }

    private BehandlingRepository lagMockedRepositoryForOpprettingAvBehandlingInternt() {
        if (mockBehandlingRepository != null && behandling != null) {
            return mockBehandlingRepository;
        }
        validerTilstandVedMocking();

        mockBehandlingRepository = mockBehandlingRepository();

        lagre(repositoryProvider); // NOSONAR //$NON-NLS-1$
        return mockBehandlingRepository;
    }

    public Behandling lagMocked() {
        lagMockedRepositoryForOpprettingAvBehandlingInternt();
        return behandling;
    }

    private void lagrePersonopplysning(BehandlingRepositoryProvider repositoryProvider, Behandling behandling) {
        PersonopplysningRepository personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        Long behandlingId = behandling.getId();

        if (personer != null && !personer.isEmpty()) {
            personer.stream().filter(e -> e.getType().equals(PersonopplysningVersjonType.REGISTRERT))
                .findFirst().ifPresent(e -> lagrePersoninfo(behandling, e, personopplysningRepository));

            personer.stream().filter(a -> a.getType().equals(PersonopplysningVersjonType.OVERSTYRT))
                .findFirst().ifPresent(b -> {
                    if (personer.stream().noneMatch(c -> c.getType().equals(PersonopplysningVersjonType.REGISTRERT))) {
                        personopplysningRepository.opprettBuilderFraEksisterende(behandlingId, PersonopplysningVersjonType.OVERSTYRT);
                    }
                    lagrePersoninfo(behandling, b, personopplysningRepository);
                });

        } else {
            PersonInformasjon registerInformasjon = PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT)
                .leggTilPersonopplysninger(
                    Personopplysning.builder()
                        .aktørId(behandling.getAktørId())
                        .navn("Forelder")
                        .fødselsdato(LocalDate.now().minusYears(25))
                        .sivilstand(SivilstandType.UOPPGITT)
                        .region(Region.NORDEN))
                .leggTilPersonstatus(Personstatus.builder()
                    .personstatus(PersonstatusType.BOSA)
                    .periode(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
                    .aktørId(behandling.getAktørId()))
                .build();
            lagrePersoninfo(behandling, registerInformasjon, personopplysningRepository);
        }
    }

    private void lagrePersoninfo(Behandling behandling, PersonInformasjon personInformasjon, PersonopplysningRepository repository) {
        Objects.requireNonNull(behandling);
        Objects.requireNonNull(personInformasjon);

        if (personInformasjon.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
            lagreRegisterPersoninfo(behandling, personInformasjon, repository);
        } else {
            lagreOverstyrtPersoninfo(behandling, personInformasjon, repository);
        }
    }

    private void lagreRegisterPersoninfo(Behandling behandling, PersonInformasjon personInformasjon, PersonopplysningRepository repository) {
        lagrePersoninfo(behandling, repository.opprettBuilderFraEksisterende(behandling.getId(), PersonopplysningVersjonType.REGISTRERT), personInformasjon, repository);
    }

    private void lagreOverstyrtPersoninfo(Behandling behandling, PersonInformasjon personInformasjon, PersonopplysningRepository repository) {
        lagrePersoninfo(behandling, repository.opprettBuilderFraEksisterende(behandling.getId(), PersonopplysningVersjonType.OVERSTYRT), personInformasjon, repository);
    }

    private void lagrePersoninfo(Behandling behandling, PersonInformasjonBuilder personInformasjonBuilder, PersonInformasjon personInformasjon,
                                 PersonopplysningRepository repository) {
        personInformasjon.getPersonopplysninger().forEach(e -> {
            PersonInformasjonBuilder.PersonopplysningBuilder builder = personInformasjonBuilder.getPersonopplysningBuilder(e.getAktørId());
            builder.medNavn(e.getNavn())
                .medFødselsdato(e.getFødselsdato())
                .medDødsdato(e.getDødsdato())
                .medKjønn(e.getBrukerKjønn())
                .medRegion(e.getRegion())
                .medSivilstand(e.getSivilstand());

            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getAdresser().forEach(e -> {
            PersonInformasjonBuilder.AdresseBuilder builder = personInformasjonBuilder.getAdresseBuilder(e.getAktørId(), e.getPeriode(), e.getAdresseType());
            builder.medAdresselinje1(e.getAdresselinje1())
                .medLand(e.getLand())
                .medPostnummer(e.getPostnummer());
            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getPersonstatuser().forEach(e -> {
            PersonInformasjonBuilder.PersonstatusBuilder builder = personInformasjonBuilder.getPersonstatusBuilder(e.getAktørId(), e.getPeriode());
            builder.medPersonstatus(e.getPersonstatus());
            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getStatsborgerskap().forEach(e -> {
            Region region = Region.finnHøyestRangertRegion(List.of(e.getStatsborgerskap().getKode()));
            PersonInformasjonBuilder.StatsborgerskapBuilder builder = personInformasjonBuilder.getStatsborgerskapBuilder(e.getAktørId(), e.getPeriode(),
                e.getStatsborgerskap(), region);
            personInformasjonBuilder.leggTil(builder);
        });

        personInformasjon.getRelasjoner().forEach(e -> {
            PersonInformasjonBuilder.RelasjonBuilder builder = personInformasjonBuilder.getRelasjonBuilder(e.getAktørId(), e.getTilAktørId(),
                e.getRelasjonsrolle());
            builder.harSammeBosted(e.getHarSammeBosted());
            personInformasjonBuilder.leggTil(builder);
        });

        repository.lagre(behandling.getId(), personInformasjonBuilder);
    }

    private void validerTilstandVedMocking() {
        if (startSteg != null) {
            throw new IllegalArgumentException(
                "Kan ikke sette startSteg ved mocking siden dette krever Kodeverk.  Bruk ManipulerInternBehandling til å justere etterpå.");
        }
    }

    @SuppressWarnings("unchecked")
    public S medSøknadDato(LocalDate søknadsdato) {
        medSøknad().medSøknadsdato(søknadsdato);
        return (S) this;
    }

    private void build(BehandlingRepository behandlingRepo, BehandlingRepositoryProvider repositoryProvider) {
        if (behandling != null) {
            throw new IllegalStateException("build allerede kalt.  Hent Behandling via getBehandling eller opprett nytt scenario.");
        }
        Builder behandlingBuilder = grunnBuild(repositoryProvider);

        this.behandling = behandlingBuilder.build();
        this.behandling.setBehandlingResultatType(behandlingResultatType);

        if (startSteg != null) {
            new InternalManipulerBehandling().forceOppdaterBehandlingSteg(behandling, startSteg);
        }

        leggTilAksjonspunkter(behandling);

        BehandlingLås lås = behandlingRepo.taSkriveLås(behandling);
        behandlingRepo.lagre(behandling, lås);

        lagrePersonopplysning(repositoryProvider, behandling);
        lagreSøknad(repositoryProvider);
        // opprett og lagre resulater på behandling
        lagreVilkårResultat(repositoryProvider, lås);

        if (this.opplysningerOppdatertTidspunkt != null) {
            behandlingRepo.oppdaterSistOppdatertTidspunkt(this.behandling, this.opplysningerOppdatertTidspunkt);
        }

        // få med behandlingsresultat etc.
        behandlingRepo.lagre(behandling, lås);
    }

    private void leggTilAksjonspunkter(Behandling behandling) {
        aksjonspunktDefinisjoner.forEach(
            (apDef, stegType) -> {
                if (stegType != null) {
                    new AksjonspunktTestSupport().leggTilAksjonspunkt(behandling, apDef, stegType);
                } else {
                    new AksjonspunktTestSupport().leggTilAksjonspunkt(behandling, apDef);
                }
            });
    }

    private void lagreSøknad(BehandlingRepositoryProvider repositoryProvider) {
        if (søknadBuilder != null) {
            final SøknadRepository søknadRepository = repositoryProvider.getSøknadRepository();
            søknadRepository.lagreOgFlush(behandling, søknadBuilder.build());
        }
    }

    private Builder grunnBuild(BehandlingRepositoryProvider repositoryProvider) {
        FagsakRepository fagsakRepo = repositoryProvider.getFagsakRepository();

        lagFagsak(fagsakRepo);

        // oppprett og lagre behandling
        Builder behandlingBuilder;
        if (originalBehandling == null) {
            behandlingBuilder = Behandling.nyBehandlingFor(fagsak, behandlingType);
        } else {
            behandlingBuilder = Behandling.fraTidligereBehandling(originalBehandling, behandlingType)
                .medBehandlingÅrsak(
                    BehandlingÅrsak.builder(behandlingÅrsakType).medManueltOpprettet(manueltOpprettet));
        }

        if (behandlingstidFrist != null) {
            behandlingBuilder.medBehandlingstidFrist(behandlingstidFrist);
        }

        if (behandlendeEnhet != null) {
            behandlingBuilder.medBehandlendeEnhet(new OrganisasjonsEnhet(behandlendeEnhet, null));
        }

        behandlingBuilder.medBehandlingStatus(behandlingStatus);

        return behandlingBuilder;

    }

    private void lagFagsak(FagsakRepository fagsakRepo) {
        if (fagsak != null)
            return;

        // opprett og lagre fagsak. Må gjøres før kan opprette behandling
        fagsak = fagsakBuilder.build();
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(12), LocalDate.now().plusMonths(12));
        FagsakTestUtil.oppdaterPeriode(fagsak, periode);
        Long fagsakId = fagsakRepo.opprettNy(fagsak); // NOSONAR //$NON-NLS-1$
        fagsak.setId(fagsakId);
    }

    private void lagreVilkårResultat(BehandlingRepositoryProvider repoProvider, BehandlingLås lås) {
        VilkårResultatBuilder inngangsvilkårBuilder = Vilkårene.builder();

        vilkår.forEach(v -> {
            inngangsvilkårBuilder.leggTil(inngangsvilkårBuilder.hentBuilderFor(v.getVilkårType()).leggTil(new VilkårPeriodeBuilder()
                .medPeriode(DatoIntervallEntitet.fra(v.getPeriode()))
                .medUtfall(v.getUtfall())));
        });

        final var build = inngangsvilkårBuilder.build();

        repoProvider.getVilkårResultatRepository().lagre(behandling.getId(), build);

        if (behandlingVedtakBuilder != null) {
            // Må lagre Behandling for at Behandlingsresultat ikke skal være transient når BehandlingVedtak blir lagret:
            behandlingVedtak = behandlingVedtakBuilder.medBehandling(behandling.getId()).build();
            repoProvider.getBehandlingVedtakRepository().lagre(behandlingVedtak, lås);
        }
    }

    public Fagsak getFagsak() {
        if (fagsak == null) {
            throw new IllegalStateException("Kan ikke hente Fagsak før denne er bygd");
        }
        return fagsak;
    }

    public AktørId getDefaultBrukerAktørId() {
        return fagsakBuilder.getAktørId();
    }

    public Behandling getBehandling() {
        if (behandling == null) {
            throw new IllegalStateException("Kan ikke hente Behandling før denne er bygd");
        }
        return behandling;
    }

    @SuppressWarnings("unchecked")
    public S medSaksnummer(Saksnummer saksnummer) {
        fagsakBuilder.medSaksnummer(saksnummer);
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medFagsakId(Long id) {
        this.fagsakId = id;
        return (S) this;
    }

    public BehandlingVedtak.Builder medBehandlingVedtak() {
        if (behandlingVedtakBuilder == null) {
            behandlingVedtakBuilder = BehandlingVedtak.builder()
                // Setter defaultverdier
                .medVedtakstidspunkt(LocalDateTime.now().minusDays(1))
                .medAnsvarligSaksbehandler("Nav Navesen");
        }
        return behandlingVedtakBuilder;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingStatus(BehandlingStatus status) {
        this.behandlingStatus = Objects.requireNonNull(status, "status");
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingsresultat(BehandlingResultatType behandlingResultatType) {
        this.behandlingResultatType = behandlingResultatType;
        return (S) this;
    }

    public SøknadEntitet.Builder medSøknad() {
        if (søknadBuilder == null) {
            søknadBuilder = new SøknadEntitet.Builder()
                .medJournalpostId(new JournalpostId(nyId()));
        }
        return søknadBuilder;
    }

    protected void utenSøknad() {
        this.søknadBuilder = null;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingType(BehandlingType behandlingType) {
        this.behandlingType = behandlingType;
        return (S) this;
    }

    public S leggTilVilkår(VilkårType vilkårType, Utfall utfall) {
        return this.leggTilVilkår(vilkårType, utfall, new Periode(LocalDate.now().minusMonths(3), LocalDate.now()));
    }

    @SuppressWarnings("unchecked")
    public S leggTilVilkår(VilkårType vilkårType, Utfall utfall, Periode periode) {
        vilkår.add(new VilkårData(vilkårType, utfall, periode));
        return (S) this;
    }

    public void leggTilAksjonspunkt(AksjonspunktDefinisjon apDef, BehandlingStegType stegType) {
        aksjonspunktDefinisjoner.put(apDef, stegType);
    }

    @SuppressWarnings("unchecked")
    public S medBruker(AktørId aktørId) {
        fagsakBuilder
            .medBruker(aktørId);

        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingStegStart(BehandlingStegType startSteg) {
        this.startSteg = startSteg;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlingstidFrist(LocalDate behandlingstidFrist) {
        this.behandlingstidFrist = behandlingstidFrist;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medBehandlendeEnhet(String behandlendeEnhet) {
        this.behandlendeEnhet = behandlendeEnhet;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medOpplysningerOppdatertTidspunkt(LocalDateTime opplysningerOppdatertTidspunkt) {
        this.opplysningerOppdatertTidspunkt = opplysningerOppdatertTidspunkt;
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S medRegisterOpplysninger(PersonInformasjon personinfo) {
        Objects.requireNonNull(personinfo);
        if (!personinfo.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
            throw new IllegalStateException("Feil versjontype, må være PersonopplysningVersjonType.REGISTRERT");
        }
        if (this.personer == null) {
            this.personer = new ArrayList<>();
            this.personer.add(personinfo);
        }
        return (S) this;
    }

    public PersonInformasjon.Builder opprettBuilderForRegisteropplysninger() {
        if (personInformasjonBuilder == null) {
            personInformasjonBuilder = PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT);
        }
        return personInformasjonBuilder;
    }

    public S medOriginalBehandling(Behandling originalBehandling, BehandlingÅrsakType behandlingÅrsakType) {
        return medOriginalBehandling(originalBehandling, behandlingÅrsakType, false);
    }

    @SuppressWarnings("unchecked")
    private S medOriginalBehandling(Behandling originalBehandling, BehandlingÅrsakType behandlingÅrsakType, boolean manueltOpprettet) {
        this.originalBehandling = originalBehandling;
        this.fagsak = originalBehandling.getFagsak();
        this.behandlingÅrsakType = behandlingÅrsakType;
        this.manueltOpprettet = manueltOpprettet;
        return (S) this;
    }

    static class VilkårData {
        private Periode periode;
        private Utfall utfall;
        private VilkårType vilkårType;

        VilkårData(VilkårType vilkårType, Utfall utfall, Periode periode) {
            super();
            this.periode = periode;
            this.utfall = utfall;
            this.vilkårType = vilkårType;
        }

        Periode getPeriode() {
            return periode;
        }

        Utfall getUtfall() {
            return utfall;
        }

        VilkårType getVilkårType() {
            return vilkårType;
        }

    }

    private final class MockPersonopplysningRepository extends PersonopplysningRepository {
        @Override
        public void kopierGrunnlagFraEksisterendeBehandlingForRevurdering(Long eksisterendeBehandlingId, Long nyBehandlingId) {
            final PersonopplysningGrunnlagBuilder oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
                Optional.ofNullable(personopplysningMap.getOrDefault(eksisterendeBehandlingId, null)));

            personopplysningMap.put(nyBehandlingId, oppdatere.build());
        }

        @Override
        public Optional<PersonopplysningGrunnlagEntitet> hentPersonopplysningerHvisEksisterer(Long behandlingId) {
            return Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null));
        }

        @Override
        public PersonopplysningGrunnlagEntitet hentPersonopplysninger(Long behandlingId) {
            if (personopplysningMap.isEmpty() || personopplysningMap.get(behandlingId) == null || !personopplysningMap.containsKey(behandlingId)) {
                throw new IllegalStateException("Fant ingen personopplysninger for angitt behandling");
            }

            return personopplysningMap.getOrDefault(behandlingId, null);
        }

        @Override
        public DiffResult diffResultat(PersonopplysningGrunnlagEntitet grunnlag1, PersonopplysningGrunnlagEntitet grunnlag2, boolean kunSporedeEndringer) {
            return null;
        }

        @Override
        public void lagre(Long behandlingId, PersonInformasjonBuilder builder) {
            final PersonopplysningGrunnlagBuilder oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
                Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null)));
            if (builder.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
                oppdatere.medRegistrertVersjon(builder);
            }
            if (builder.getType().equals(PersonopplysningVersjonType.OVERSTYRT)) {
                oppdatere.medOverstyrtVersjon(builder);
            }
            personopplysningMap.put(behandlingId, oppdatere.build());
        }

        @Override
        public PersonInformasjonBuilder opprettBuilderFraEksisterende(Long behandlingId, PersonopplysningVersjonType type) {
            final Optional<PersonopplysningGrunnlagEntitet> aktivtGrunnlag = Optional.ofNullable(personopplysningMap.getOrDefault(behandlingId, null));
            if (aktivtGrunnlag.isEmpty()) {
                return new PersonInformasjonBuilder(type);
            }

            var eksisterende = type == PersonopplysningVersjonType.REGISTRERT
                ? aktivtGrunnlag.get().getRegisterVersjon()
                : aktivtGrunnlag.get().getOverstyrtVersjon();
            return eksisterende.isEmpty() ? new PersonInformasjonBuilder(type) : new PersonInformasjonBuilder(eksisterende.get(), type);
        }

        @Override
        public void kopierGrunnlagFraEksisterendeBehandling(Long gammelBehandlingId, Long nyBehandlingId) {
            final PersonopplysningGrunnlagBuilder oppdatere = PersonopplysningGrunnlagBuilder.oppdatere(
                Optional.ofNullable(personopplysningMap.getOrDefault(gammelBehandlingId, null)));

            personopplysningMap.put(nyBehandlingId, oppdatere.build());
        }

        @Override
        public PersonopplysningGrunnlagEntitet hentFørsteVersjonAvPersonopplysninger(Long behandlingId) {
            throw new java.lang.UnsupportedOperationException("Ikke implementert");
        }

        @Override
        public PersonopplysningGrunnlagEntitet hentPersonopplysningerPåId(Long aggregatId) {
            throw new java.lang.UnsupportedOperationException("Ikke implementert");
        }
    }

}
